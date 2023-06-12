/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.event.impl;

import java.lang.invoke.MethodHandles;
import java.util.BitSet;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
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
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoTypeIndexingPlan;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
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
public final class HibernateSearchEventListener
		implements PostDeleteEventListener,
		PostInsertEventListener, PostUpdateEventListener,
		PostCollectionRecreateEventListener, PostCollectionRemoveEventListener, PostCollectionUpdateEventListener,
		FlushEventListener, AutoFlushEventListener, ClearEventListener {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmListenerContextProvider contextProvider;
	private final boolean dirtyCheckingEnabled;

	public HibernateSearchEventListener(HibernateOrmListenerContextProvider contextProvider,
			boolean dirtyCheckingEnabled) {
		this.contextProvider = contextProvider;
		this.dirtyCheckingEnabled = dirtyCheckingEnabled;
		log.debug( "Hibernate Search dirty checks " + ( dirtyCheckingEnabled ? "enabled" : "disabled" ) );
	}

	public void registerTo(SessionFactoryImplementor sessionFactory) {
		EventListenerRegistry listenerRegistry =
				HibernateOrmUtils.getServiceOrFail( sessionFactory.getServiceRegistry(), EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy(
				new KeepIfSameClassDuplicationStrategy( HibernateSearchEventListener.class ) );

		listenerRegistry.appendListeners( EventType.POST_INSERT, this );
		listenerRegistry.appendListeners( EventType.POST_UPDATE, this );
		listenerRegistry.appendListeners( EventType.POST_DELETE, this );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_RECREATE, this );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_REMOVE, this );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_UPDATE, this );
		listenerRegistry.appendListeners( EventType.FLUSH, this );
		listenerRegistry.appendListeners( EventType.AUTO_FLUSH, this );
		listenerRegistry.appendListeners( EventType.CLEAR, this );
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		if ( !contextProvider.listenerEnabled() ) {
			return;
		}
		Object entity = event.getEntity();
		HibernateOrmListenerTypeContext typeContext = getTypeContextOrNull( event.getPersister() );
		if ( typeContext == null ) {
			return;
		}
		PojoTypeIndexingPlan plan = getCurrentIndexingPlanIfTypeIncluded( event.getSession(), typeContext );
		if ( plan == null ) {
			// This type is excluded through filters.
			// Return early, to avoid unnecessary processing.
			return;
		}

		Object providedId = typeContext.toIndexingPlanProvidedId( event.getId() );
		plan.delete( providedId, null, entity );
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		if ( !contextProvider.listenerEnabled() ) {
			return;
		}
		final Object entity = event.getEntity();
		HibernateOrmListenerTypeContext typeContext = getTypeContextOrNull( event.getPersister() );
		if ( typeContext == null ) {
			return;
		}
		PojoTypeIndexingPlan plan = getCurrentIndexingPlanIfTypeIncluded( event.getSession(), typeContext );
		if ( plan == null ) {
			// This type is excluded through filters.
			// Return early, to avoid unnecessary processing.
			return;
		}

		Object providedId = typeContext.toIndexingPlanProvidedId( event.getId() );
		plan.add( providedId, null, entity );

		BitSet dirtyAssociationPaths = typeContext.dirtyContainingAssociationFilter().all();

		// In case ToOne associations are updated on the "contained" side only,
		// make sure to remember that the association was updated on the "containing" side too.
		// This will only work correctly if the containing side of the association
		// is lazy and has not yet been loaded (otherwise it will be out of date when reindexing)
		// but that's the best we can do.
		if ( dirtyAssociationPaths != null ) {
			plan.updateAssociationInverseSide( dirtyAssociationPaths, null, event.getState() );
		}
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		if ( !contextProvider.listenerEnabled() ) {
			return;
		}
		final Object entity = event.getEntity();
		HibernateOrmListenerTypeContext typeContext = getTypeContextOrNull( event.getPersister() );
		if ( typeContext == null ) {
			// This type is not indexed, nor contained in an indexed type.
			// Return early, to avoid creating an indexing plan.
			return;
		}
		PojoTypeIndexingPlan plan = getCurrentIndexingPlanIfTypeIncluded( event.getSession(), typeContext );
		if ( plan == null ) {
			// This type is excluded through filters.
			// Return early, to avoid unnecessary processing.
			return;
		}

		boolean considerAllDirty;
		BitSet dirtyPaths;
		BitSet dirtyDirectAssociationPaths;
		if ( dirtyCheckingEnabled ) {
			int[] dirtyProperties = event.getDirtyProperties();
			if ( dirtyProperties == null || dirtyProperties.length == 0 ) {
				// No information about dirty properties.
				// This can happen when an entity is merged before it's even been loaded.
				// Just assume everything is dirty.
				considerAllDirty = true;
				dirtyPaths = null;
				dirtyDirectAssociationPaths = typeContext.dirtyContainingAssociationFilter().all();
			}
			else {
				considerAllDirty = false;
				dirtyPaths = typeContext.dirtyFilter().filter( dirtyProperties );
				dirtyDirectAssociationPaths = typeContext.dirtyContainingAssociationFilter().filter( dirtyProperties );
			}
		}
		else {
			// Dirty checking is disabled.
			// Just assume everything is dirty.
			considerAllDirty = true;
			dirtyPaths = null;
			dirtyDirectAssociationPaths = typeContext.dirtyContainingAssociationFilter().all();
		}

		Object providedId = typeContext.toIndexingPlanProvidedId( event.getId() );
		if ( considerAllDirty ) {
			plan.addOrUpdate( providedId, null, entity, true, true, null );
		}
		else if ( dirtyPaths != null ) {
			plan.addOrUpdate( providedId, null, entity, false, false, dirtyPaths );
		}

		// In case ToOne associations are updated on the "contained" side only,
		// make sure to remember that the association was updated on the "containing" side too.
		// This will only work correctly if the containing side of the association
		// is lazy and has not yet been loaded (otherwise it will be out of date when reindexing)
		// but that's the best we can do.
		if ( dirtyDirectAssociationPaths != null ) {
			plan.updateAssociationInverseSide( dirtyDirectAssociationPaths, event.getOldState(), event.getState() );
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
		if ( !contextProvider.listenerEnabled() ) {
			return;
		}
		EventSource session = event.getSession();

		PojoIndexingPlan plan = getCurrentIndexingPlanIfExisting( session );
		if ( plan == null ) {
			// Nothing to flush
			return;
		}

		plan.process();

		// flush within a transaction should trigger only the prepare phase,
		// since the execute phase is supposed to be triggered by the transaction commit
		if ( !session.isTransactionInProgress() ) {
			// out of transaction it will trigger both of them
			contextProvider.currentAutomaticIndexingSynchronizationStrategy( session )
					.executeAndSynchronize( plan );
		}
	}

	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		if ( !contextProvider.listenerEnabled() ) {
			return;
		}
		if ( !event.isFlushRequired() ) {
			/*
			 * Auto-flush was disabled or there wasn't any entity/collection to flush.
			 * Note this is not an optimization: we really need to avoid triggering entity processing in this case.
			 * There may be dirty entities, but ORM chose not to flush them,
			 * so we shouldn't flush the index changes either.
			 */
			return;
		}
		PojoIndexingPlan plan = getCurrentIndexingPlanIfExisting( event.getSession() );
		if ( plan != null ) {
			plan.process();
		}
	}

	@Override
	public void onClear(ClearEvent event) {
		if ( !contextProvider.listenerEnabled() ) {
			return;
		}
		EventSource session = event.getSession();
		PojoIndexingPlan plan = getCurrentIndexingPlanIfExisting( session );

		// skip the clearNotPrepared operation in case there has been no one to clear
		if ( plan != null ) {
			plan.discardNotProcessed();
		}
	}

	private PojoTypeIndexingPlan getCurrentIndexingPlanIfTypeIncluded(SessionImplementor sessionImplementor,
			HibernateOrmListenerTypeContext typeContext) {
		return contextProvider.currentIndexingPlanIfTypeIncluded( sessionImplementor, typeContext.typeIdentifier() );
	}

	private PojoIndexingPlan getCurrentIndexingPlanIfExisting(SessionImplementor sessionImplementor) {
		return contextProvider.currentIndexingPlanIfExisting( sessionImplementor );
	}

	private HibernateOrmListenerTypeContext getTypeContextOrNull(EntityMappingType entityMappingType) {
		String entityName = entityMappingType.getEntityName();
		return contextProvider.typeContextProvider().byHibernateOrmEntityName().getOrNull( entityName );
	}

	private void processCollectionEvent(AbstractCollectionEvent event) {
		if ( !contextProvider.listenerEnabled() ) {
			return;
		}
		Object ownerEntity = event.getAffectedOwnerOrNull();
		if ( ownerEntity == null ) {
			//Hibernate cannot determine every single time the owner especially in case detached objects are involved
			// or property-ref is used
			//Should log really but we don't know if we're interested in this collection for indexing
			return;
		}

		HibernateOrmListenerTypeContext typeContext = contextProvider.typeContextProvider()
				.byHibernateOrmEntityName().getOrNull( event.getAffectedOwnerEntityName() );
		if ( typeContext == null ) {
			// This type is not indexed, nor contained in an indexed type.
			// Return early, to avoid creating an indexing plan.
			return;
		}
		PojoTypeIndexingPlan plan = getCurrentIndexingPlanIfTypeIncluded( event.getSession(), typeContext );
		if ( plan == null ) {
			// This type is excluded through filters.
			// Return early, to avoid unnecessary processing.
			return;
		}

		BitSet dirtyPaths;
		if ( dirtyCheckingEnabled ) {
			PersistentCollection persistentCollection = event.getCollection();
			String collectionRole = null;
			if ( persistentCollection != null ) {
				collectionRole = persistentCollection.getRole();
			}
			if ( collectionRole != null ) {
				// Collection role will only be non-null for PostCollectionUpdateEvents.
				// For those events, we can determine whether the collection has any impact on indexing.
				dirtyPaths = typeContext.dirtyFilter().filter( collectionRole );
				if ( dirtyPaths == null ) {
					// This collection is not relevant for indexing.
					// Return early, to avoid creating an indexing plan.
					return;
				}
			}
			else {
				// We don't know which collection is being changed,
				// so we have to default to reindexing, just in case.
				dirtyPaths = null;
			}
		}
		else {
			// Dirty checking is disabled.
			// Just assume everything is dirty.
			dirtyPaths = null;
		}

		Object providedId = typeContext.toIndexingPlanProvidedId( event.getAffectedOwnerIdOrNull() );
		if ( dirtyPaths != null ) {
			plan.addOrUpdate( providedId, null, ownerEntity, false, false, dirtyPaths );
		}
		else {
			plan.addOrUpdate( providedId, null, ownerEntity, true, true, null );
		}
	}

	/**
	 * Required since Hibernate ORM 4.3
	 */
	@Override
	@SuppressForbiddenApis(reason = "We are forced to implement this method and it requires accepting an EntityPersister")
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		// TODO Tests seem to pass using _false_ but we might be able to take
		// advantage of this new hook?
		return false;
	}
}
