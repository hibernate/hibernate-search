/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.route.impl.DocumentRouteImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 * @param <R> The type of entity references returned in the {@link #executeAndReport() failure report}.
 */
public class PojoIndexedTypeIndexingPlan<I, E, R>
		extends AbstractPojoTypeIndexingPlan<I, E, PojoIndexedTypeIndexingPlan<I, E, R>.IndexedEntityState> {

	private final PojoWorkIndexedTypeContext<I, E> typeContext;
	private final IndexIndexingPlan<R> delegate;

	public PojoIndexedTypeIndexingPlan(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext<?> sessionContext,
			IndexIndexingPlan<R> delegate) {
		super( sessionContext );
		this.typeContext = typeContext;
		this.delegate = delegate;
	}

	@Override
	void purge(Object providedId, String providedRoutingKey) {
		I identifier = typeContext.identifierMapping().getIdentifier( providedId );
		getState( identifier ).purge( providedRoutingKey );
	}

	void updateBecauseOfContained(Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( null, entitySupplier );
		if ( !statesPerId.containsKey( identifier ) ) {
			getState( identifier ).updateBecauseOfContained( entitySupplier );
		}
		// If the entry is already there, no need for an additional update
	}

	@Override
	void resolveDirty(PojoReindexingCollector containingEntityCollector) {
		// We need to iterate on a "frozen snapshot" of the states because of HSEARCH-3857
		List<IndexedEntityState> frozenIndexingPlansPerId = new ArrayList<>( statesPerId.values() );
		for ( IndexedEntityState plan : frozenIndexingPlansPerId ) {
			plan.resolveDirty( containingEntityCollector );
		}
	}

	void discard() {
		delegate.discard();
	}

	void discardNotProcessed() {
		this.statesPerId.clear();
	}

	void process() {
		sendCommandsToDelegate();
		delegate.process();
	}

	CompletableFuture<IndexIndexingPlanExecutionReport<R>> executeAndReport() {
		sendCommandsToDelegate();
		/*
		 * No need to call prepare() here:
		 * delegates are supposed to handle execute() even without a prior call to prepare().
		 */
		return delegate.executeAndReport();
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

	private void sendCommandsToDelegate() {
		try {
			statesPerId.values().forEach( IndexedEntityState::sendCommandsToDelegate );
		}
		finally {
			statesPerId.clear();
		}
	}

	class IndexedEntityState
			extends AbstractPojoTypeIndexingPlan<I, E, IndexedEntityState>.AbstractEntityState {

		private String providedRoutingKey;

		private boolean updatedBecauseOfContained;

		private IndexedEntityState(I identifier) {
			super( identifier );
		}

		@Override
		void add(Supplier<E> entitySupplier, String providedRoutingKey) {
			super.add( entitySupplier, providedRoutingKey );
			this.providedRoutingKey = providedRoutingKey;
		}

		@Override
		void doUpdate(Supplier<E> entitySupplier, String providedRoutingKey) {
			super.doUpdate( entitySupplier, providedRoutingKey );
			this.providedRoutingKey = providedRoutingKey;
		}

		void updateBecauseOfContained(Supplier<E> entitySupplier) {
			doUpdate( entitySupplier, null );
			updatedBecauseOfContained = true;
			// We don't want contained entities that haven't been modified to trigger an update of their
			// containing entities.
			// Thus we don't set 'shouldResolveToReindex' to true here, but leave it as is.
		}

		@Override
		void delete(Supplier<E> entitySupplier, String providedRoutingKey) {
			super.delete( entitySupplier, providedRoutingKey );
			this.providedRoutingKey = providedRoutingKey;

			// Reindexing does not make sense for a deleted entity
			updatedBecauseOfContained = false;
		}

		void purge(String providedRoutingKey) {
			// This is a purge: assume the document exists in order to force deletion.
			this.initialStatus = EntityStatus.PRESENT;
			delete( null, providedRoutingKey );
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
									|| typeContext.requiresSelfReindexing( dirtyPaths ) ) {
								delegateUpdate();
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
			PojoWorkRouter router = typeContext.createRouter( sessionContext, identifier, entitySupplier );
			DocumentRouteImpl currentRoute = router.currentRoute( providedRoutingKey );
			// We don't care about previous routes: the add() operation expects that the document isn't in the index yet.

			String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );

			if ( currentRoute == null ) {
				// The routing bridge decided the entity should not be indexed.
				// There's nothing to do.
				return;
			}
			DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
					currentRoute.routingKey(), identifier );
			delegate.add( referenceProvider,
					typeContext.toDocumentContributor( sessionContext, identifier, entitySupplier ) );
		}

		private void delegateUpdate() {
			PojoWorkRouter router = typeContext.createRouter( sessionContext, identifier, entitySupplier );
			DocumentRouteImpl currentRoute = router.currentRoute( providedRoutingKey );
			List<DocumentRouteImpl> previousRoutes = router.previousRoutes( currentRoute );

			String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );

			delegateDeletePrevious( documentIdentifier, previousRoutes );

			if ( currentRoute == null ) {
				// The routing bridge decided the entity should not be indexed.
				// We should have deleted it using the "previous routes" (if it was actually indexed previously),
				// and we don't have anything else to do.
				return;
			}
			DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
					currentRoute.routingKey(), identifier );
			delegate.update( referenceProvider,
					typeContext.toDocumentContributor( sessionContext, identifier, entitySupplier ) );
		}

		private void delegateDelete() {
			String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );

			if ( entitySupplier == null ) {
				// Purge: entity is not available and we can't route automatically.
				DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
						providedRoutingKey, identifier );
				delegate.delete( referenceProvider );
				return;
			}

			PojoWorkRouter router = typeContext.createRouter( sessionContext, identifier, entitySupplier );
			DocumentRouteImpl currentRoute = router.currentRoute( providedRoutingKey );
			List<DocumentRouteImpl> previousRoutes = router.previousRoutes( currentRoute );

			delegateDeletePrevious( documentIdentifier, previousRoutes );

			if ( currentRoute == null ) {
				// The routing bridge decided the entity should not be indexed.
				// We should have deleted it using the "previous routes" (if it was actually indexed previously).
				return;
			}

			DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
					currentRoute.routingKey(), identifier );
			delegate.delete( referenceProvider );
		}

		private void delegateDeletePrevious(String documentIdentifier,
				List<DocumentRouteImpl> previousRoutes) {
			for ( DocumentRouteImpl route : previousRoutes ) {
				DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
						route.routingKey(), identifier );
				delegate.delete( referenceProvider );
			}
		}
	}

}
