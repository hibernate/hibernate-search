/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.DocumentRouter;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.NoOpDocumentRouter;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 */
public class PojoIndexedTypeIndexingPlan<I, E>
		extends AbstractPojoTypeIndexingPlan<I, E, PojoIndexedTypeIndexingPlan<I, E>.IndexedEntityState> {

	private final PojoWorkIndexedTypeContext<I, E> typeContext;
	private final PojoTypeIndexingPlanDelegate<I, E> delegate;

	public PojoIndexedTypeIndexingPlan(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext, PojoIndexingPlanImpl root,
			PojoTypeIndexingPlanDelegate<I, E> delegate) {
		super( sessionContext, root );
		this.typeContext = typeContext;
		this.delegate = delegate;
	}

	void updateBecauseOfContained(Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( null, entitySupplier );
		getState( identifier ).updateBecauseOfContained( entitySupplier );
	}

	@Override
	void resolveDirty() {
		// We need to iterate on a "frozen snapshot" of the states because of HSEARCH-3857
		List<IndexedEntityState> frozenIndexingPlansPerId = new ArrayList<>( statesPerId.values() );
		for ( IndexedEntityState plan : frozenIndexingPlansPerId ) {
			plan.resolveDirty();
		}
	}

	void discard() {
		delegate.discard();
	}

	void discardNotProcessed() {
		this.statesPerId.clear();
	}

	void process() {
		try {
			statesPerId.values().forEach( IndexedEntityState::sendCommandsToDelegate );
		}
		finally {
			statesPerId.clear();
		}
	}

	<R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory) {
		process();
		return delegate.executeAndReport( entityReferenceFactory );
	}

	@Override
	PojoWorkIndexedTypeContext<I, E> typeContext() {
		return typeContext;
	}

	@Override
	I toIdentifier(Object providedId, Supplier<E> entitySupplier) {
		return typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );
	}

	@Override
	protected IndexedEntityState createState(I identifier) {
		return new IndexedEntityState( identifier );
	}

	class IndexedEntityState
			extends AbstractPojoTypeIndexingPlan<I, E, IndexedEntityState>.AbstractEntityState {

		private DocumentRoutesDescriptor providedRoutes;

		private boolean updatedBecauseOfContained;

		private IndexedEntityState(I identifier) {
			super( identifier );
		}

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

		@Override
		void delete(Supplier<E> entitySupplier) {
			super.delete( entitySupplier );

			// Reindexing does not make sense for a deleted entity
			updatedBecauseOfContained = false;
		}

		@Override
		void providedRoutes(DocumentRoutesDescriptor routes) {
			if ( routes == null ) {
				return;
			}
			if ( this.providedRoutes == null ) {
				this.providedRoutes = routes;
			}
			else {
				Set<DocumentRouteDescriptor> mergedPrevious = new LinkedHashSet<>( this.providedRoutes.previousRoutes() );
				mergedPrevious.addAll( routes.previousRoutes() );
				this.providedRoutes = DocumentRoutesDescriptor.of( routes.currentRoute(), mergedPrevious );
			}
		}

		void sendCommandsToDelegate() {
			switch ( currentStatus ) {
				case UNKNOWN:
					// No operation was called on this state.
					// Don't do anything.
					return;
				case PRESENT:
					switch ( initialStatus ) {
						case ABSENT:
							delegateAdd();
							return;
						case PRESENT:
						case UNKNOWN:
							if ( considerAllDirty || updatedBecauseOfContained
									|| dirtyPaths != null && typeContext.dirtySelfFilter().test( dirtyPaths ) ) {
								delegateAddOrUpdate();
							}
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

		private void delegateAdd() {
			Supplier<E> entitySupplier = entitySupplierOrLoad();
			if ( entitySupplier == null ) {
				// We couldn't retrieve the entity.
				// Assume it was deleted and there's nothing to add.
				// A delete event should follow at some point.
				return;
			}

			DocumentRouteDescriptor currentRoute = typeContext.router()
					.currentRoute( identifier, entitySupplier, providedRoutes, sessionContext );
			// We don't care about previous routes: the add() operation expects that the document isn't in the index yet.
			if ( currentRoute == null ) {
				// The routing bridge decided the entity should not be indexed.
				// There's nothing to do.
				return;
			}
			delegate.add( identifier, entitySupplier, currentRoute );
		}

		private void delegateAddOrUpdate() {
			Supplier<E> entitySupplier = entitySupplierOrLoad();
			if ( entitySupplier == null ) {
				// We couldn't retrieve the entity.
				// Assume it was deleted and there's nothing to add or update.
				// A delete event should follow at some point.
				return;
			}

			DocumentRoutesDescriptor routes = typeContext.router()
					.routes( identifier, entitySupplier, providedRoutes, sessionContext );
			if ( routes.currentRoute() == null && routes.previousRoutes().isEmpty() ) {
				// The routing bridge decided the entity should not be indexed, and that it wasn't indexed previously.
				// There's nothing to do.
				return;
			}
			delegate.addOrUpdate( identifier, routes, entitySupplier );
		}

		private void delegateDelete() {
			Supplier<E> entitySupplier = entitySupplierOrLoad();

			DocumentRouter<? super E> router;
			if ( entitySupplier != null ) {
				router = typeContext.router();
			}
			else {
				// Purge: entity is not available and we can't route according to its state.
				// We can use the provided routing keys, though, which is what the no-op router does.
				router = NoOpDocumentRouter.INSTANCE;
			}

			DocumentRoutesDescriptor routes = router
					.routes( identifier, entitySupplier, providedRoutes, sessionContext );
			if ( routes.currentRoute() == null && routes.previousRoutes().isEmpty() ) {
				// The routing bridge decided the entity should not be indexed, and that it wasn't indexed previously.
				// There's nothing to do.
				return;
			}
			delegate.delete( identifier, routes, entitySupplier );
		}
	}

}
