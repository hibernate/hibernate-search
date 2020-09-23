/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class PojoIndexedTypeIndexingPlan<I, E, R> extends AbstractPojoTypeIndexingPlan {

	private final PojoWorkIndexedTypeContext<I, E> typeContext;
	private final IndexIndexingPlan<R> delegate;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<I, IndexedEntityIndexingPlan> indexingPlansPerId = new LinkedHashMap<>();

	public PojoIndexedTypeIndexingPlan(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext<?> sessionContext,
			IndexIndexingPlan<R> delegate) {
		super( sessionContext );
		this.typeContext = typeContext;
		this.delegate = delegate;
	}

	@Override
	void add(Object providedId, String providedRoutingKey, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );
		getPlan( identifier ).add( entitySupplier, providedRoutingKey );
	}

	@Override
	void update(Object providedId, String providedRoutingKey, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );
		getPlan( identifier ).update( entitySupplier, providedRoutingKey );
	}

	@Override
	void update(Object providedId, String providedRoutingKey, Object entity, String... dirtyPaths) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );
		getPlan( identifier ).update( entitySupplier, providedRoutingKey, dirtyPaths );
	}

	@Override
	void delete(Object providedId, String providedRoutingKey, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );
		getPlan( identifier ).delete( entitySupplier, providedRoutingKey );
	}

	@Override
	void purge(Object providedId, String providedRoutingKey) {
		I identifier = typeContext.identifierMapping().getIdentifier( providedId );
		getPlan( identifier ).purge( providedRoutingKey );
	}

	void updateBecauseOfContained(Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( null, entitySupplier );
		if ( !indexingPlansPerId.containsKey( identifier ) ) {
			getPlan( identifier ).updateBecauseOfContained( entitySupplier );
		}
		// If the entry is already there, no need for an additional update
	}

	void resolveDirty(PojoReindexingCollector containingEntityCollector) {
		// We need to iterate on a "frozen snapshot" of the indexingPlansPerId values
		// because of HSEARCH-3857
		List<IndexedEntityIndexingPlan> frozenIndexingPlansPerId = new ArrayList<>( indexingPlansPerId.values() );
		for ( IndexedEntityIndexingPlan plan : frozenIndexingPlansPerId ) {
			plan.resolveDirty( containingEntityCollector );
		}
	}

	void process() {
		sendCommandsToDelegate();
		getDelegate().process();
	}

	CompletableFuture<IndexIndexingPlanExecutionReport<R>> executeAndReport() {
		sendCommandsToDelegate();
		/*
		 * No need to call prepare() here:
		 * delegates are supposed to handle execute() even without a prior call to prepare().
		 */
		return delegate.executeAndReport();
	}

	void discard() {
		delegate.discard();
	}

	void discardNotProcessed() {
		this.indexingPlansPerId.clear();
	}

	private IndexedEntityIndexingPlan getPlan(I identifier) {
		IndexedEntityIndexingPlan plan = indexingPlansPerId.get( identifier );
		if ( plan == null ) {
			plan = new IndexedEntityIndexingPlan( identifier );
			indexingPlansPerId.put( identifier, plan );
		}
		return plan;
	}

	private IndexIndexingPlan<?> getDelegate() {
		return delegate;
	}

	private void sendCommandsToDelegate() {
		try {
			indexingPlansPerId.values().forEach( IndexedEntityIndexingPlan::sendCommandsToDelegate );
		}
		finally {
			indexingPlansPerId.clear();
		}
	}

	private class IndexedEntityIndexingPlan {
		private final I identifier;
		private String providedRoutingKey;
		private Supplier<E> entitySupplier;

		private boolean delete;
		private boolean add;

		private boolean shouldResolveToReindex;
		private boolean considerAllDirty;
		private boolean updatedBecauseOfContained;
		private Set<String> dirtyPaths;

		private IndexedEntityIndexingPlan(I identifier) {
			this.identifier = identifier;
		}

		void add(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			this.providedRoutingKey = providedRoutingKey;
			shouldResolveToReindex = true;
			add = true;
		}

		void update(Supplier<E> entitySupplier, String providedRoutingKey) {
			doUpdate( entitySupplier, providedRoutingKey );
			shouldResolveToReindex = true;
			considerAllDirty = true;
			dirtyPaths = null;
		}

		void update(Supplier<E> entitySupplier, String providedRoutingKey, String... dirtyPaths) {
			doUpdate( entitySupplier, providedRoutingKey );
			shouldResolveToReindex = true;
			if ( !considerAllDirty ) {
				for ( String dirtyPropertyName : dirtyPaths ) {
					addDirtyPath( dirtyPropertyName );
				}
			}
		}

		void updateBecauseOfContained(Supplier<E> entitySupplier) {
			doUpdate( entitySupplier, null );
			updatedBecauseOfContained = true;
			/*
			 * We don't want contained entities that haven't been modified to trigger an update of their
			 * containing entities.
			 * Thus we don't set 'shouldResolveToReindex' to true here, but leave it as is.
			 */
		}

		void delete(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			this.providedRoutingKey = providedRoutingKey;
			if ( add && !delete ) {
				/*
				 * We called add() in the same plan, so we don't expect the document to be in the index.
				 * Don't delete, just cancel the addition.
				 */
				add = false;
				delete = false;
			}
			else {
				add = false;
				delete = true;
			}

			// Reindexing does not make sense for a deleted entity
			shouldResolveToReindex = false;
			considerAllDirty = false;
			updatedBecauseOfContained = false;
			dirtyPaths = null;
		}

		void purge(String providedRoutingKey) {
			entitySupplier = null;
			this.providedRoutingKey = providedRoutingKey;
			// This is a purge: do not resolve reindexing
			shouldResolveToReindex = false;
			// This is a purge: force deletion even if it doesn't seem this document was added
			considerAllDirty = false;
			dirtyPaths = null;
			add = false;
			delete = true;
		}

		void resolveDirty(PojoReindexingCollector containingEntityCollector) {
			if ( shouldResolveToReindex ) {
				shouldResolveToReindex = false; // Avoid infinite looping
				typeContext.resolveEntitiesToReindex(
						containingEntityCollector, sessionContext, identifier, entitySupplier,
						considerAllDirty ? null : dirtyPaths
				);
			}
		}

		void sendCommandsToDelegate() {
			if ( add ) {
				if ( delete ) {
					if ( considerAllDirty || updatedBecauseOfContained
							|| typeContext.requiresSelfReindexing( dirtyPaths ) ) {
						delegateUpdate();
					}
				}
				else {
					delegateAdd();
				}
			}
			else if ( delete ) {
				delegateDelete();
			}
		}

		private void doUpdate(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			this.providedRoutingKey = providedRoutingKey;
			/*
			 * If add is true, either this is already an update (in which case we don't need to change the flags)
			 * or we called add() in the same plan (in which case we don't expect the document to be in the index).
			 */
			if ( !add ) {
				delete = true;
				add = true;
			}
		}

		private void addDirtyPath(String dirtyPath) {
			if ( dirtyPaths == null ) {
				dirtyPaths = new HashSet<>();
			}
			dirtyPaths.add( dirtyPath );
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
