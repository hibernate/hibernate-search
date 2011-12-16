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
package org.hibernate.search.event.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.jmx.IndexControl;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.Session;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.backend.impl.EventSourceTransactionContext;
import org.hibernate.search.cfg.impl.SearchConfigurationFromHibernateCore;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.util.impl.WeakIdentityHashMap;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.event.impl.FullTextIndexEventListener.Installation.SINGLE_INSTANCE;

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
//TODO make this class final as soon as FullTextIndexCollectionEventListener is removed.
@SuppressWarnings("serial")
public class FullTextIndexEventListener implements PostDeleteEventListener,
		PostInsertEventListener, PostUpdateEventListener,
		PostCollectionRecreateEventListener, PostCollectionRemoveEventListener,
		PostCollectionUpdateEventListener, FlushEventListener {

	private static final Log log = LoggerFactory.make();
	private final Installation installation;

	protected boolean used;
	protected boolean skipDirtyChecks = true;
	protected SearchFactoryImplementor searchFactoryImplementor;

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

	public FullTextIndexEventListener(Installation installation) {
		this.installation = installation;
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

		String enableJMX = cfg.getProperty( Environment.JMX_ENABLED );
		if ( "true".equalsIgnoreCase( enableJMX ) ) {
			enableIndexControlBean( cfg );
		}

		String indexingStrategy = searchFactoryImplementor.getIndexingStrategy();
		if ( "event".equals( indexingStrategy ) ) {
			used = searchFactoryImplementor.getIndexBindingForEntity().size() != 0;
		}
		else if ( "manual".equals( indexingStrategy ) ) {
			used = false;
		}

		log.debug( "Hibernate Search event listeners " + ( used ? "activated" : "deactivated" ) );

		skipDirtyChecks = !searchFactoryImplementor.isDirtyChecksEnabled();
		log.debug( "Hibernate Search dirty checks " + ( skipDirtyChecks ? "disabled" : "enabled" ) );
	}

	private void enableIndexControlBean(Configuration cfg) {

		// if we don't have a JNDI bound SessionFactory we cannot enable the index control bean
		if ( StringHelper.isEmpty( cfg.getProperty( "hibernate.session_factory_name" ) ) ) {
			log.debug(
					"In order to bind the IndexControlMBean the Hibernate SessionFactory has to be available via JNDI"
			);
			return;
		}

		// since the SearchFactory is mutable we might have an already existing MBean which we have to unregister first
		if ( JMXRegistrar.isNameRegistered( IndexControl.INDEX_CTRL_MBEAN_OBJECT_NAME ) ) {
			JMXRegistrar.unRegisterMBean( IndexControl.INDEX_CTRL_MBEAN_OBJECT_NAME );
		}

		IndexControl indexCtrlBean = new IndexControl( cfg.getProperties() );
		JMXRegistrar.registerMBean( indexCtrlBean, IndexControl.INDEX_CTRL_MBEAN_OBJECT_NAME );
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
			if ( docBuilder != null && ( skipDirtyChecks || docBuilder.isDirty( getDirtyPropertyNames(
					event
			) ) ) ) {
				Serializable id = event.getId();
				processWork( entity, id, WorkType.UPDATE, event, false );
			}
		}
	}

    public String[] getDirtyPropertyNames(PostUpdateEvent event) {
		EntityPersister persister = event.getPersister();
		final int[] dirtyProperties = event.getDirtyProperties();
		if ( dirtyProperties != null && dirtyProperties.length > 0 ) {
			String[] propertyNames = persister.getPropertyNames();
			int length = dirtyProperties.length;
			String[] dirtyPropertyNames = new String[length];
			for ( int i = 0; i < length; i++ ) {
				dirtyPropertyNames[i] = propertyNames[dirtyProperties[i]];
			}
			return dirtyPropertyNames;
		}
		else {
			return null;
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
				if ( ! persistentCollection.wasInitialized() ) {
					// non-initialized collections will still trigger events, but we want to skip them
					// as they won't contain new values affecting the index state
					return;
				}
				collectionRole = persistentCollection.getRole();
			}
			else {
				collectionRole = null;
			}
			AbstractDocumentBuilder<?> documentBuilder = getDocumentBuilder( entity );
			
			if ( documentBuilder != null && ! documentBuilder.isCollectionRoleExcluded( collectionRole ) ) {
				Serializable id = getId( entity, event );
				if ( id == null ) {
					log.idCannotBeExtracted( event.getAffectedOwnerEntityName() );
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

	/**
	 * It is not suggested to extend FullTextIndexEventListener, but when needed to implement special
	 * use cases implementors might need this method. If you have to extent this, please report
	 * your use case so that better long term solutions can be discussed.
	 * @param entity
	 * @return the DocumentBuilder for the specified entity
	 */
	protected AbstractDocumentBuilder getDocumentBuilder(final Object entity) {
		Class<?> clazz = entity.getClass();
		EntityIndexBinder entityIndexBinding = searchFactoryImplementor.getIndexBindingForEntity( clazz );
		if ( entityIndexBinding != null ) {
			return entityIndexBinding.getDocumentBuilder();
		}
		else {
			return searchFactoryImplementor.getDocumentBuilderContainedEntity( clazz );
		}
	}

	public static enum Installation {
		SINGLE_INSTANCE
	}
}
