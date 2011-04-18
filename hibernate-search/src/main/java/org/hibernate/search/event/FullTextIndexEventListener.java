/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.slf4j.Logger;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.EntityEntry;
import org.hibernate.event.AbstractCollectionEvent;
import org.hibernate.event.AbstractEvent;
import org.hibernate.event.Destructible;
import org.hibernate.event.EventSource;
import org.hibernate.event.FlushEvent;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.Initializable;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.backend.impl.EventSourceTransactionContext;
import org.hibernate.search.cfg.SearchConfigurationFromHibernateCore;
import org.hibernate.search.engine.AbstractDocumentBuilder;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.ReflectionHelper;
import org.hibernate.search.util.WeakIdentityHashMap;

import static org.hibernate.search.event.FullTextIndexEventListener.Installation.SINGLE_INSTANCE;

/**
 * This listener supports setting a parent directory for all generated index files.
 * It also supports setting the analyzer class to be used.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Mattias Arbin
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
//TODO implement and use a LockableDirectoryProvider that wraps a DP to handle the lock inside the LDP
//TODO make this class final as soon as FullTextIndexCollectionEventListener is removed.
@SuppressWarnings("serial")
public class FullTextIndexEventListener implements PostDeleteEventListener,
		PostInsertEventListener, PostUpdateEventListener,
		PostCollectionRecreateEventListener, PostCollectionRemoveEventListener,
		PostCollectionUpdateEventListener, FlushEventListener, Initializable, Destructible {

	private static final Logger log = LoggerFactory.make();
	private final Installation installation;

	protected boolean used;
	protected boolean skipDirtyChecks = true;
	protected SearchFactoryImplementor searchFactoryImplementor;
	private final DirtyStrategy dirtyStrategy;

	static {
		Version.touch();
	}

	//only used by the FullTextIndexEventListener instance playing in the FlushEventListener role.
	// transient because it's not serializable (and state doesn't need to live longer than a flush).
	// final because it's initialization should be published to other threads.
	// ! update the readObject() method in case of name changes !
	// make sure the Synchronization doesn't contain references to Session, otherwise we'll leak memory.
	private transient final Map<Session, Synchronization> flushSynch = new WeakIdentityHashMap<Session, Synchronization>(
			0
	);

	/**
	 * @deprecated As of Hibernate Search 3.3. This method was used for instantiating the event listener when configured
	 *             in a configuration file. Since Hibernate Core 3.6 Hibernate Search will always be automatically enabled if available
	 *             on the classpath.
	 */
	public FullTextIndexEventListener() {
		String msg = "FullTextIndexEventListener default constructor is obsolete. Remove all explicit" +
				"event listener configuration. As of Hibernate Core 3.6 Hibernate Search will be automatically enabled " +
				"if it is detected on the classpath.";
		log.error( msg );
		throw new SearchException( msg );
	}

	public FullTextIndexEventListener(Installation installation) {
		this.installation = installation;
		//TODO remove this code when moving to Core 4 (HSEARCH-660)
		DirtyStrategy dirtyStrategy;
		try {
			PostUpdateEvent.class.getMethod( "getDirtyProperties" );
			dirtyStrategy = new CoreComputedDirtyStrategy();
		}
		catch ( NoSuchMethodException e ) {
			dirtyStrategy = new HSearchComputedDirtyStrategy();
		}
		this.dirtyStrategy = dirtyStrategy;
	}

	/**
	 * Initialize method called by Hibernate Core when the SessionFactory starts
	 */

	public void initialize(Configuration cfg) {
		if ( installation != SINGLE_INSTANCE ) {
			throw new SearchException( "Only Installation.SINGLE_INSTANCE is supported" );
		}

		if ( searchFactoryImplementor == null ) {
			searchFactoryImplementor = new SearchFactoryBuilder()
					.configuration( new SearchConfigurationFromHibernateCore( cfg ) )
					.buildSearchFactory();
		}

		String indexingStrategy = searchFactoryImplementor.getIndexingStrategy();
		if ( "event".equals( indexingStrategy ) ) {
			used = searchFactoryImplementor.getDocumentBuildersIndexedEntities().size() != 0;
		}
		else if ( "manual".equals( indexingStrategy ) ) {
			used = false;
		}

		log.debug( "Hibernate Search event listeners " + ( used ? "activated" : "deactivated" ) );

		skipDirtyChecks = !searchFactoryImplementor.isDirtyChecksEnabled();
		log.debug( "Hibernate Search dirty checks " + ( skipDirtyChecks ? "disabled" : "enabled" ) );
	}

	public SearchFactoryImplementor getSearchFactoryImplementor() {
		return searchFactoryImplementor;
	}

	public void onPostDelete(PostDeleteEvent event) {
		if ( used ) {
			final Object entity = event.getEntity();
			if ( getDocumentBuilder( entity ) != null ) {
				// FIXME The engine currently needs to know about details such as identifierRollbackEnabled
				// but we should not move the responsibility to figure out the proper id to the engine  
				boolean identifierRollbackEnabled = event.getSession()
						.getFactory()
						.getSettings()
						.isIdentifierRollbackEnabled();
				processWork( entity, event.getId(), WorkType.DELETE, event, identifierRollbackEnabled );
			}
		}
	}

	public void onPostInsert(PostInsertEvent event) {
		if ( used ) {
			final Object entity = event.getEntity();
			if ( getDocumentBuilder( entity ) != null ) {
				Serializable id = event.getId();
				processWork( entity, id, WorkType.ADD, event, false );
			}
		}
	}

	public void onPostUpdate(PostUpdateEvent event) {
		if ( used ) {
			final Object entity = event.getEntity();
			final AbstractDocumentBuilder docBuilder = getDocumentBuilder( entity );
			if ( docBuilder != null && ( skipDirtyChecks || docBuilder.isDirty( dirtyStrategy.getDirtyPropertyNames(
					event
			) ) ) ) {
				Serializable id = event.getId();
				processWork( entity, id, WorkType.UPDATE, event, false );
			}
		}
	}

	protected <T> void processWork(T entity, Serializable id, WorkType workType, AbstractEvent event, boolean identifierRollbackEnabled) {
		Work<T> work = new Work<T>( entity, id, workType, identifierRollbackEnabled );
		final EventSourceTransactionContext transactionContext = new EventSourceTransactionContext( event.getSession() );
		searchFactoryImplementor.getWorker().performWork( work, transactionContext );
	}

	public void cleanup() {
		searchFactoryImplementor.close();
	}

	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		processCollectionEvent( event );
	}

	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		processCollectionEvent( event );
	}

	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		processCollectionEvent( event );
	}

	protected void processCollectionEvent(AbstractCollectionEvent event) {
		if ( used ) {
			Object entity = event.getAffectedOwnerOrNull();
			if ( entity == null ) {
				//Hibernate cannot determine every single time the owner especially in case detached objects are involved
				// or property-ref is used
				//Should log really but we don't know if we're interested in this collection for indexing
				return;
			}
			PersistentCollection persistentCollection = event.getCollection();
			final String collectionRole;
			if ( persistentCollection != null ) {
				collectionRole = persistentCollection.getRole();
			}
			else {
				collectionRole = null;
			}
			AbstractDocumentBuilder<?> documentBuilder = getDocumentBuilder( entity );
			
			if ( documentBuilder != null && ! documentBuilder.isCollectionRoleExcluded( collectionRole ) ) {
				Serializable id = getId( entity, event );
				if ( id == null ) {
					log.warn(
							"Unable to reindex entity on collection change, id cannot be extracted: {}",
							event.getAffectedOwnerEntityName()
					);
					return;
				}
				processWork( entity, id, WorkType.COLLECTION, event, false );
			}
		}
	}

	private Serializable getId(Object entity, AbstractCollectionEvent event) {
		Serializable id = event.getAffectedOwnerIdOrNull();
		if ( id == null ) {
			// most likely this recovery is unnecessary since Hibernate Core probably try that
			EntityEntry entityEntry = event.getSession().getPersistenceContext().getEntry( entity );
			id = entityEntry == null ? null : entityEntry.getId();
		}
		return id;
	}

	/**
	 * Make sure the indexes are updated right after the hibernate flush,
	 * avoiding object loading during a flush. Not needed during transactions.
	 */
	public void onFlush(FlushEvent event) {
		if ( used ) {
			Session session = event.getSession();
			Synchronization synchronization = flushSynch.get( session );
			if ( synchronization != null ) {
				//first cleanup
				flushSynch.remove( session );
				log.debug( "flush event causing index update out of transaction" );
				synchronization.beforeCompletion();
				synchronization.afterCompletion( Status.STATUS_COMMITTED );
			}
		}
	}

	/**
	 * Adds a synchronization to be performed in the onFlush method;
	 * should only be used as workaround for the case a flush is happening
	 * out of transaction.
	 * Warning: if the synchronization contains a hard reference
	 * to the Session proper cleanup is not guaranteed and memory leaks
	 * will happen.
	 *
	 * @param eventSource should be the Session doing the flush
	 * @param synchronization the synchronisation instance
	 */
	public void addSynchronization(EventSource eventSource, Synchronization synchronization) {
		this.flushSynch.put( eventSource, synchronization );
	}

	private void writeObject(ObjectOutputStream os) throws IOException {
		os.defaultWriteObject();
	}

	//needs to implement custom readObject to restore the transient fields

	private void readObject(ObjectInputStream is)
			throws IOException, ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		is.defaultReadObject();
		Class<FullTextIndexEventListener> cl = FullTextIndexEventListener.class;
		Field f = cl.getDeclaredField( "flushSynch" );
		ReflectionHelper.setAccessible( f );
		Map<Session, Synchronization> flushSynch = new WeakIdentityHashMap<Session, Synchronization>( 0 );
		// setting a final field by reflection during a readObject is considered as safe as in a constructor:
		f.set( this, flushSynch );
	}

	private AbstractDocumentBuilder getDocumentBuilder(final Object entity) {
		Class<?> clazz = entity.getClass();
		AbstractDocumentBuilder documentBuilderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity(
				clazz
		);
		if ( documentBuilderIndexedEntity != null ) {
			return documentBuilderIndexedEntity;
		}
		else {
			return searchFactoryImplementor.getDocumentBuilderContainedEntity( clazz );
		}
	}

	public static enum Installation {
		SINGLE_INSTANCE,

		/**
		 * @see #FullTextIndexEventListener()
		 * @deprecated As of Hibernate Search 3.3.
		 */
		MULTIPLE_INSTANCE
	}
}
