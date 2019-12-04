/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.event.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
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
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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
public final class HibernateSearchEventListener implements PostDeleteEventListener,
		PostInsertEventListener, PostUpdateEventListener,
		PostCollectionRecreateEventListener, PostCollectionRemoveEventListener, PostCollectionUpdateEventListener,
		FlushEventListener, AutoFlushEventListener, ClearEventListener {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private final boolean dirtyCheckingEnabled;

	private volatile EventsHibernateSearchState state;

	public HibernateSearchEventListener(
			CompletableFuture<? extends HibernateOrmListenerContextProvider> contextProviderFuture,
			boolean dirtyCheckingEnabled) {
		this.state = new InitializingHibernateSearchState( contextProviderFuture.thenApply( this::doInitialize ) );
		this.dirtyCheckingEnabled = dirtyCheckingEnabled;
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		HibernateOrmListenerContextProvider contextProvider = state.getContextProvider();
		Object entity = event.getEntity();
		HibernateOrmListenerTypeContext typeContext = getTypeContext( contextProvider, event.getPersister() );
		if ( typeContext != null ) {
			Object providedId = typeContext.toIndexingPlanProvidedId( event.getId() );
			// TODO Check whether deletes work with hibernate.use_identifier_rollback enabled (see HSEARCH-650)
			// I think they should, but better safe than sorry
			getCurrentIndexingPlan( contextProvider, event.getSession() )
					.delete( typeContext.getTypeIdentifier(), providedId, entity );
		}
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		HibernateOrmListenerContextProvider contextProvider = state.getContextProvider();
		final Object entity = event.getEntity();
		HibernateOrmListenerTypeContext typeContext = getTypeContext( contextProvider, event.getPersister() );
		if ( typeContext != null ) {
			Object providedId = typeContext.toIndexingPlanProvidedId( event.getId() );
			getCurrentIndexingPlan( contextProvider, event.getSession() )
					.add( typeContext.getTypeIdentifier(), providedId, entity );
		}
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		HibernateOrmListenerContextProvider contextProvider = state.getContextProvider();
		final Object entity = event.getEntity();
		HibernateOrmListenerTypeContext typeContext = getTypeContext( contextProvider, event.getPersister() );
		if ( typeContext != null ) {
			PojoIndexingPlan plan = getCurrentIndexingPlan( contextProvider, event.getSession() );
			Object providedId = typeContext.toIndexingPlanProvidedId( event.getId() );
			if ( dirtyCheckingEnabled ) {
				plan.addOrUpdate( typeContext.getTypeIdentifier(), providedId, entity, getDirtyPropertyNames( event ) );
			}
			else {
				plan.addOrUpdate( typeContext.getTypeIdentifier(), providedId, entity );
			}
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
	 * avoiding entity loading during a flush. Not needed during transactions.
	 */
	@Override
	public void onFlush(FlushEvent event) {
		HibernateOrmListenerContextProvider contextProvider = state.getContextProvider();
		EventSource session = event.getSession();

		PojoIndexingPlan plan = getCurrentIndexingPlanIfExisting( contextProvider, session );
		if ( plan == null ) {
			// Nothing to flush
			return;
		}

		plan.process();

		// flush within a transaction should trigger only the prepare phase,
		// since the execute phase is supposed to be triggered by the transaction commit
		if ( !session.isTransactionInProgress() ) {
			// out of transaction it will trigger both of them
			contextProvider.getCurrentAutomaticIndexingSynchronizationStrategy( session )
					.executeAndSynchronize( plan );
		}
	}

	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		if ( !event.isFlushRequired() ) {
			/*
			 * Auto-flush was disabled or there wasn't any entity/collection to flush.
			 * Note this is not an optimization: we really need to avoid triggering entity processing in this case.
			 * There may be dirty entities, but ORM chose not to flush them,
			 * so we shouldn't flush the index changes either.
			 */
			return;
		}
		getCurrentIndexingPlan( state.getContextProvider(), event.getSession() ).process();
	}

	@Override
	public void onClear(ClearEvent event) {
		EventSource session = event.getSession();
		PojoIndexingPlan plan = getCurrentIndexingPlanIfExisting( state.getContextProvider(), session );

		// skip the clearNotPrepared operation in case there has been no one to clear
		if ( plan != null ) {
			plan.discardNotProcessed();
		}
	}

	private HibernateOrmListenerContextProvider doInitialize(
			HibernateOrmListenerContextProvider contextProvider) {
		log.debug( "Hibernate Search dirty checks " + ( dirtyCheckingEnabled ? "enabled" : "disabled" ) );
		// discard the suboptimal EventsHibernateSearchState instances
		this.state = new OptimalEventsHibernateSearchState( contextProvider );
		return contextProvider;
	}

	private PojoIndexingPlan getCurrentIndexingPlan(HibernateOrmListenerContextProvider contextProvider,
			SessionImplementor sessionImplementor) {
		return contextProvider.getCurrentIndexingPlan( sessionImplementor, true );
	}

	private PojoIndexingPlan getCurrentIndexingPlanIfExisting(HibernateOrmListenerContextProvider contextProvider,
			SessionImplementor sessionImplementor) {
		return contextProvider.getCurrentIndexingPlan( sessionImplementor, false );
	}

	private HibernateOrmListenerTypeContext getTypeContext(HibernateOrmListenerContextProvider contextProvider,
			EntityPersister entityPersister) {
		String entityName = entityPersister.getEntityName();
		return contextProvider.getTypeContextByHibernateOrmEntityName( entityName );
	}

	private void processCollectionEvent(AbstractCollectionEvent event) {
		HibernateOrmListenerContextProvider contextProvider = state.getContextProvider();
		Object ownerEntity = event.getAffectedOwnerOrNull();
		if ( ownerEntity == null ) {
			//Hibernate cannot determine every single time the owner especially in case detached objects are involved
			// or property-ref is used
			//Should log really but we don't know if we're interested in this collection for indexing
			return;
		}

		HibernateOrmListenerTypeContext typeContext =
				contextProvider.getTypeContextByHibernateOrmEntityName( event.getAffectedOwnerEntityName() );
		if ( typeContext != null ) {
			PojoIndexingPlan plan = getCurrentIndexingPlan( contextProvider, event.getSession() );
			Object providedId = typeContext.toIndexingPlanProvidedId( event.getAffectedOwnerIdOrNull() );

			if ( dirtyCheckingEnabled ) {
				PersistentCollection persistentCollection = event.getCollection();
				String collectionRole = null;
				if ( persistentCollection != null ) {
					collectionRole = persistentCollection.getRole();
				}
				if ( collectionRole != null ) {
					/*
					 * Collection role will only be non-null for PostCollectionUpdateEvents.
					 * For those events, we can pass the role to the indexing plan
					 * which can then decide whether to reindex based on whether the collection
					 * has any impact on indexing.
					 */
					plan.addOrUpdate( typeContext.getTypeIdentifier(), providedId, ownerEntity, collectionRole );
				}
				else {
					/*
					 * We don't know which collection is being changed,
					 * so we have to default to reindexing, just in case.
					 */
					plan.addOrUpdate( typeContext.getTypeIdentifier(), providedId, ownerEntity );
				}
			}
			else {
				plan.addOrUpdate( typeContext.getTypeIdentifier(), providedId, ownerEntity );
			}
		}
	}

	private String[] getDirtyPropertyNames(PostUpdateEvent event) {
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
			return EMPTY_STRING_ARRAY;
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
}
