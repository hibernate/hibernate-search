/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.event.impl;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.Session;
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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.util.impl.Maps;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Hibernate ORM event listener called by various ORM life cycle events. This listener must be registered in order
 * to enable automatic index updates.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Mattias Arbin
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
@SuppressWarnings("serial")
public final class FullTextIndexEventListener implements PostDeleteEventListener,
		PostInsertEventListener, PostUpdateEventListener,
		PostCollectionRecreateEventListener, PostCollectionRemoveEventListener,
		PostCollectionUpdateEventListener, FlushEventListener {

	private static final Log log = LoggerFactory.make();

	private volatile EventsIntegratorState state = new NonInitializedIntegratorState();

	//only used by the FullTextIndexEventListener instance playing in the FlushEventListener role.
	// make sure the Synchronization doesn't contain references to Session, otherwise we'll leak memory.
	private final Map<Session, Synchronization> flushSynch = Maps.createIdentityWeakKeyConcurrentMap( 64, 32 );

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		if ( state.eventsDisabled() ) {
			return;
		}

		final Object entity = event.getEntity();
		if ( getDocumentBuilder( entity ) != null ) {
			// FIXME The engine currently needs to know about details such as identifierRollbackEnabled
			// but we should not move the responsibility to figure out the proper id to the engine
			boolean identifierRollbackEnabled = event.getSession()
					.getFactory()
					.getSessionFactoryOptions()
					.isIdentifierRollbackEnabled();
			processWork( tenantIdentifier( event ), entity, event.getId(), WorkType.DELETE, event, identifierRollbackEnabled );
		}
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		if ( state.eventsDisabled() ) {
			return;
		}

		final Object entity = event.getEntity();
		if ( getDocumentBuilder( entity ) != null ) {
			Serializable id = event.getId();
			processWork( tenantIdentifier( event ), entity, id, WorkType.ADD, event, false );
		}
	}

	private String tenantIdentifier(AbstractEvent event) {
		EventSource session = event.getSession();
		return session.getTenantIdentifier();
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		if ( state.eventsDisabled() ) {
			return;
		}

		final Object entity = event.getEntity();
		final AbstractDocumentBuilder docBuilder = getDocumentBuilder( entity );
		if ( docBuilder != null && ( state.skipDirtyChecks() || docBuilder.isDirty(
				getDirtyPropertyNames(
						event
				)
		) ) ) {
			Serializable id = event.getId();
			processWork( tenantIdentifier( event ), entity, id, WorkType.UPDATE, event, false );
		}
	}

	@Override
	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		processCollectionEvent( event );
	}

	@Override
	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		processCollectionEvent( event );
	}

	@Override
	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		processCollectionEvent( event );
	}

	/**
	 * Make sure the indexes are updated right after the hibernate flush,
	 * avoiding object loading during a flush. Not needed during transactions.
	 */
	@Override
	public void onFlush(FlushEvent event) {
		if ( state.eventsDisabled() ) {
			return;
		}

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

	public ExtendedSearchIntegrator getExtendedSearchFactoryIntegrator() {
		return state.getExtendedSearchIntegrator();
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

	/**
	 * Initialize method called by Hibernate Core when the SessionFactory starts.
	 * @param extendedIntegratorFuture a completable future that will eventually hold the {@link ExtendedSearchIntegrator}
	 */
	public void initialize(CompletableFuture<ExtendedSearchIntegrator> extendedIntegratorFuture) {
		this.state = new InitializingIntegratorState( extendedIntegratorFuture.thenApply( this::doInitialize ) );
	}

	private ExtendedSearchIntegrator doInitialize(ExtendedSearchIntegrator extendedIntegrator) {
		final boolean disabled = eventsDisabled( extendedIntegrator );
		final boolean skipDirtyChecks = skipDirtyChecks( extendedIntegrator );
		log.debug( "Hibernate Search event listeners " + ( disabled ? "deactivated" : "activated" ) );
		if ( ! disabled ) {
			log.debug( "Hibernate Search dirty checks " + ( skipDirtyChecks ? "disabled" : "enabled" ) );
		}
		OptimalEventsIntegratorState newState = new OptimalEventsIntegratorState( disabled, skipDirtyChecks, extendedIntegrator );
		this.state = newState; // discard the suboptimal EventsIntegratorState instances
		return extendedIntegrator;
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

	protected void processWork(String tenantIdentifier, Object entity, Serializable id, WorkType workType, AbstractEvent event, boolean identifierRollbackEnabled) {
		Work work = new Work( tenantIdentifier, entity, id, workType, identifierRollbackEnabled );
		final EventSourceTransactionContext transactionContext = new EventSourceTransactionContext( event.getSession() );
		getExtendedSearchFactoryIntegrator().getWorker().performWork( work, transactionContext );
	}

	protected void processCollectionEvent(AbstractCollectionEvent event) {
		if ( state.eventsDisabled() ) {
			return;
		}

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
		AbstractDocumentBuilder documentBuilder = getDocumentBuilder( entity );

		if ( documentBuilder != null && documentBuilder.collectionChangeRequiresIndexUpdate( collectionRole ) ) {
			Serializable id = getId( entity, event );
			if ( id == null ) {
				log.idCannotBeExtracted( event.getAffectedOwnerEntityName() );
				return;
			}
			processWork( tenantIdentifier( event ), entity, id, WorkType.COLLECTION, event, false );
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
	 * It is not suggested to extend FullTextIndexEventListener, but when needed to implement special
	 * use cases implementors might need this method. If you have to extent this, please report
	 * your use case so that better long term solutions can be discussed.
	 *
	 * @param instance the object instance for which to retrieve the document builder
	 *
	 * @return the {@code DocumentBuilder} for the specified object
	 */
	protected AbstractDocumentBuilder getDocumentBuilder(final Object instance) {
		ExtendedSearchIntegrator integrator = getExtendedSearchFactoryIntegrator();
		Class<?> clazz = instance.getClass();
		IndexedTypeIdentifier type = integrator.getIndexBindings().keyFromPojoType( clazz );
		EntityIndexBinding entityIndexBinding = integrator.getIndexBinding( type );
		if ( entityIndexBinding != null ) {
			return entityIndexBinding.getDocumentBuilder();
		}
		else {
			return integrator.getDocumentBuilderContainedEntity( type );
		}
	}

	/**
	 * Required since Hibernate ORM 4.3
	 */
	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		// TODO Tests seem to pass using _false_ but we might be able to take
		// advantage of this new hook?
		return false;
	}

	public static boolean skipDirtyChecks(ExtendedSearchIntegrator extendedIntegrator) {
		return !extendedIntegrator.isDirtyChecksEnabled();
	}

	public static boolean eventsDisabled(ExtendedSearchIntegrator extendedIntegrator) {
		if ( extendedIntegrator.getIndexingMode() == IndexingMode.EVENT ) {
			return extendedIntegrator.getIndexBindings().size() == 0;
		}
		else if ( extendedIntegrator.getIndexingMode() == IndexingMode.MANUAL ) {
			return true;
		}
		else {
			return false;
		}
	}

}
