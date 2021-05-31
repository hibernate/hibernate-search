/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.automaticindexing.spi.PojoImplicitReindexingResolverSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.DocumentRouter;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.NoOpDocumentRouter;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 * @param <S> The type of per-instance state.
 */
abstract class AbstractPojoTypeIndexingPlan<I, E, S extends AbstractPojoTypeIndexingPlan<I, E, S>.AbstractEntityState> {

	final PojoWorkSessionContext sessionContext;
	final PojoTypeIndexingPlanDelegate<I, E> delegate;

	// Use a LinkedHashMap for deterministic iteration
	final Map<I, S> statesPerId = new LinkedHashMap<>();

	AbstractPojoTypeIndexingPlan(PojoWorkSessionContext sessionContext, PojoTypeIndexingPlanDelegate<I, E> delegate) {
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	void add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.add( entitySupplier );
		state.providedRoutes( providedRoutes );
	}

	void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity, BitSet dirtyPaths,
			boolean forceSelfDirty, boolean forceContainingDirty) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.addOrUpdate( entitySupplier, dirtyPaths, forceSelfDirty, forceContainingDirty );
		state.providedRoutes( providedRoutes );
	}

	void delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.delete( entitySupplier );
		state.providedRoutes( providedRoutes );
	}

	void planLoading(PojoLoadingPlanProvider loadingPlanProvider) {
		for ( S state : statesPerId.values() ) {
			state.planLoading( loadingPlanProvider );
		}
	}

	void resolveDirty(PojoLoadingPlanProvider loadingPlanProvider, PojoReindexingCollector collector) {
		for ( S state : statesPerId.values() ) {
			state.resolveDirty( loadingPlanProvider, collector );
		}
	}

	void discard() {
		delegate.discard();
	}

	void discardNotProcessed() {
		this.statesPerId.clear();
	}

	void process(PojoLoadingPlanProvider loadingPlanProvider) {
		try {
			if ( delegate == null ) {
				// Can happen with contained types depending on the strategy.
				return;
			}
			for ( S state : statesPerId.values() ) {
				state.sendCommandsToDelegate( loadingPlanProvider );
			}
		}
		finally {
			statesPerId.clear();
		}
	}

	<R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory) {
		return delegate.executeAndReport( entityReferenceFactory );
	}

	abstract PojoWorkTypeContext<I, E> typeContext();

	abstract DocumentRouter<? super E> router();

	I toIdentifier(Object providedId, Supplier<E> entitySupplier) {
		return typeContext().identifierMapping().getIdentifier( providedId, entitySupplier );
	}

	final S getState(I identifier) {
		S state = statesPerId.get( identifier );
		if ( state == null ) {
			state = createState( identifier );
			statesPerId.put( identifier, state );
		}
		return state;
	}

	protected abstract S createState(I identifier);

	abstract class AbstractEntityState
			implements PojoImplicitReindexingResolverRootContext {
		final I identifier;
		private Supplier<E> entitySupplier;
		private Integer loadingOrdinal;

		EntityStatus initialStatus = EntityStatus.UNKNOWN;
		EntityStatus currentStatus = EntityStatus.UNKNOWN;

		private boolean shouldResolveToReindex;
		private boolean updatedBecauseOfContained;
		private boolean forceSelfDirty;
		private boolean forceContainingDirty;
		private BitSet dirtyPaths;

		AbstractEntityState(I identifier) {
			this.identifier = identifier;
		}

		@Override
		public PojoImplicitReindexingResolverSessionContext sessionContext() {
			return sessionContext;
		}

		public boolean isDirtyForAddOrUpdate() {
			return delegate.isDirtyForAddOrUpdate( forceSelfDirty, forceContainingDirty, dirtyPaths );
		}

		@Override
		public boolean isDirtyForReindexingResolution(PojoPathFilter filter) {
			return forceContainingDirty || dirtyPaths != null && filter.test( dirtyPaths );
		}

		void add(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			shouldResolveToReindex = true;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.ABSENT;
			}
			currentStatus = EntityStatus.PRESENT;
			forceSelfDirty = true;
			forceContainingDirty = true;
			dirtyPaths = null;
		}

		void addOrUpdate(Supplier<E> entitySupplier, BitSet dirtyPaths,
				boolean forceSelfDirty, boolean forceContainingDirty) {
			doAddOrUpdate( entitySupplier );
			shouldResolveToReindex = true;
			this.forceSelfDirty = this.forceSelfDirty || forceSelfDirty;
			this.forceContainingDirty = this.forceContainingDirty || forceContainingDirty;
			if ( this.forceSelfDirty && this.forceContainingDirty ) {
				this.dirtyPaths = null;
			}
			else {
				addDirtyPaths( dirtyPaths );
			}
		}

		// Should only be called on indexed types,
		// but it's simpler to implement this method for both indexed and contained types.
		void updateBecauseOfContained(Supplier<E> entitySupplier) {
			if ( currentStatus == EntityStatus.ABSENT ) {
				// This entity was deleted, but a containing entity still has a reference to it.
				// Someone probably just forgot to clear an association.
				// Just ignore the call.
				return;
			}
			doAddOrUpdate( entitySupplier );
			updatedBecauseOfContained = true;
			// We don't want contained entities that haven't been modified to trigger an update of their
			// containing entities.
			// Thus we don't set 'shouldResolveToReindex' to true here, but leave it as is.
		}

		void doAddOrUpdate(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.PRESENT;
			}
			currentStatus = EntityStatus.PRESENT;
		}

		void delete(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.PRESENT;
			}
			currentStatus = EntityStatus.ABSENT;

			// Reindexing does not make sense for a deleted entity
			shouldResolveToReindex = false;
			updatedBecauseOfContained = false;
			forceSelfDirty = false;
			forceContainingDirty = false;
			dirtyPaths = null;
		}

		abstract void providedRoutes(DocumentRoutesDescriptor routes);

		abstract DocumentRoutesDescriptor providedRoutes();

		void planLoading(PojoLoadingPlanProvider loadingPlanProvider) {
			if ( EntityStatus.PRESENT == currentStatus && entitySupplier == null ) {
				loadingOrdinal = loadingPlanProvider.loadingPlan().planLoading( typeContext(), identifier );
			}
		}

		void resolveDirty(PojoLoadingPlanProvider loadingPlanProvider, PojoReindexingCollector collector) {
			if ( shouldResolveToReindex ) {
				shouldResolveToReindex = false; // Avoid infinite looping
				Supplier<E> entitySupplier = entitySupplierOrLoad( loadingPlanProvider );
				if ( entitySupplier == null ) {
					// We couldn't retrieve the entity.
					// Assume it was deleted and there's nothing to resolve.
					return;
				}
				typeContext().resolveEntitiesToReindex( collector, sessionContext, identifier,
						entitySupplier, this
				);
			}
		}

		void sendCommandsToDelegate(PojoLoadingPlanProvider loadingPlanProvider) {
			switch ( currentStatus ) {
				case UNKNOWN:
					// No operation was called on this state.
					// Don't do anything.
					return;
				case PRESENT:
					switch ( initialStatus ) {
						case ABSENT:
							delegateAdd( loadingPlanProvider );
							return;
						case PRESENT:
						case UNKNOWN:
							delegateAddOrUpdate( loadingPlanProvider );
							return;
					}
					break;
				case ABSENT:
					switch ( initialStatus ) {
						case ABSENT:
							// The entity was added, then deleted in the same plan.
							// Don't do anything.
							return;
						case UNKNOWN:
						case PRESENT:
							delegateDelete();
							return;
					}
					break;
			}
		}

		void delegateAdd(PojoLoadingPlanProvider loadingPlanProvider) {
			Supplier<E> entitySupplier = entitySupplierOrLoad( loadingPlanProvider );
			if ( entitySupplier == null ) {
				// We couldn't retrieve the entity.
				// Assume it was deleted and there's nothing to add.
				// A delete event should follow at some point.
				return;
			}

			DocumentRouteDescriptor currentRoute = router()
					.currentRoute( identifier, entitySupplier, providedRoutes(), sessionContext );
			// We don't care about previous routes: the add() operation expects that the document isn't in the index yet.
			if ( currentRoute == null ) {
				// The routing bridge decided the entity should not be indexed.
				// There's nothing to do.
				return;
			}
			delegate.add( identifier, currentRoute, entitySupplier );
		}

		void delegateAddOrUpdate(PojoLoadingPlanProvider loadingPlanProvider) {
			boolean updateBecauseOfDirty = isDirtyForAddOrUpdate();
			if ( !updatedBecauseOfContained && !updateBecauseOfDirty ) {
				// Optimization: the update is not relevant to indexing
				return;
			}

			Supplier<E> entitySupplier = entitySupplierOrLoad( loadingPlanProvider );
			if ( entitySupplier == null ) {
				// We couldn't retrieve the entity.
				// Assume it was deleted and there's nothing to add or update.
				// A delete event should follow at some point.
				return;
			}

			DocumentRoutesDescriptor routes = router()
					.routes( identifier, entitySupplier, providedRoutes(), sessionContext );
			if ( routes.currentRoute() == null && routes.previousRoutes().isEmpty() ) {
				// The routing bridge decided the entity should not be indexed, and that it wasn't indexed previously.
				// There's nothing to do.
				return;
			}
			delegate.addOrUpdate( identifier, routes, entitySupplier,
					forceSelfDirty, forceContainingDirty, dirtyPaths,
					updatedBecauseOfContained, updateBecauseOfDirty );
		}

		void delegateDelete() {
			Supplier<E> entitySupplier = entitySupplierNoLoad();

			DocumentRouter<? super E> router;
			if ( entitySupplier != null ) {
				router = router();
			}
			else {
				// Purge: entity is not available and we can't route according to its state.
				// We can use the provided routing keys, though, which is what the no-op router does.
				router = NoOpDocumentRouter.INSTANCE;
			}

			DocumentRoutesDescriptor routes = router
					.routes( identifier, entitySupplier, providedRoutes(), sessionContext );
			if ( routes.currentRoute() == null && routes.previousRoutes().isEmpty() ) {
				// The routing bridge decided the entity should not be indexed, and that it wasn't indexed previously.
				// There's nothing to do.
				return;
			}
			delegate.delete( identifier, routes, entitySupplier );
		}

		Supplier<E> entitySupplierNoLoad() {
			return entitySupplier;
		}

		Supplier<E> entitySupplierOrLoad(PojoLoadingPlanProvider loadingPlanProvider) {
			if ( entitySupplier == null && loadingOrdinal != null ) {
				E loaded = loadingPlanProvider.loadingPlan().retrieve( typeContext(), loadingOrdinal );
				entitySupplier = typeContext().toEntitySupplier( sessionContext, loaded );
				loadingOrdinal = null;
			}
			return entitySupplier;
		}

		private void addDirtyPaths(BitSet newDirtyPaths) {
			if ( newDirtyPaths == null ) {
				return;
			}
			if ( dirtyPaths == null ) {
				dirtyPaths = new BitSet();
			}
			dirtyPaths.or( newDirtyPaths );
		}
	}

	protected enum EntityStatus {
		UNKNOWN,
		PRESENT,
		ABSENT
	}
}
