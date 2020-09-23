/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.mapper.pojo.route.impl.DocumentRouteImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

public class PojoTypeIndexer<I, E> {

	private final PojoWorkSessionContext<?> sessionContext;
	private final PojoWorkIndexedTypeContext<I, E> typeContext;
	private final IndexIndexer delegate;

	public PojoTypeIndexer(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext<?> sessionContext,
			IndexIndexer delegate) {
		this.sessionContext = sessionContext;
		this.typeContext = typeContext;
		this.delegate = delegate;
	}

	CompletableFuture<?> add(Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );

		PojoWorkRouter router = typeContext.createRouter( sessionContext, identifier, entitySupplier );
		DocumentRouteImpl currentRoute = router.currentRoute( providedRoutingKey );
		// We don't care about previous routes: the add() operation expects that the document isn't in the index yet.

		if ( currentRoute == null ) {
			// The routing bridge decided the entity should not be indexed.
			// There's nothing to do.
			return CompletableFuture.completedFuture( null );
		}
		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				currentRoute.routingKey(), identifier );
		return delegate.add( referenceProvider,
				typeContext.toDocumentContributor( sessionContext, identifier, entitySupplier ),
				commitStrategy, refreshStrategy );
	}

	CompletableFuture<?> addOrUpdate(Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );

		PojoWorkRouter router = typeContext.createRouter( sessionContext, identifier, entitySupplier );
		DocumentRouteImpl currentRoute = router.currentRoute( providedRoutingKey );
		List<DocumentRouteImpl> previousRoutes = router.previousRoutes( currentRoute );

		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );

		CompletableFuture<?> deletePreviousFuture = deletePrevious( documentIdentifier, previousRoutes, identifier,
				commitStrategy, refreshStrategy );

		if ( currentRoute == null ) {
			// The routing bridge decided the entity should not be indexed.
			// We should have deleted it using the "previous routes" (if it was actually indexed previously),
			// and we don't have anything else to do.
			return deletePreviousFuture;
		}
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				currentRoute.routingKey(), identifier );
		// Deletion on previous routes and update on current route can happen in parallel:
		// the backend is responsible for preserving relative order of works on the same index/shard + docId,
		// and we don't care about relative order of works on different indexes/shards.
		return deletePreviousFuture.thenCombine(
				delegate.update( referenceProvider,
						typeContext.toDocumentContributor( sessionContext, identifier, entitySupplier ),
						commitStrategy, refreshStrategy ),
				(deletePreviousResult, updateResult) -> updateResult );
	}

	CompletableFuture<?> delete(Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );

		PojoWorkRouter router = typeContext.createRouter( sessionContext, identifier, entitySupplier );
		DocumentRouteImpl currentRoute = router.currentRoute( providedRoutingKey );
		List<DocumentRouteImpl> previousRoutes = router.previousRoutes( currentRoute );

		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );

		CompletableFuture<?> deletePreviousFuture = deletePrevious( documentIdentifier, previousRoutes, identifier,
				commitStrategy, refreshStrategy );

		if ( currentRoute == null ) {
			// The routing bridge decided the entity should not be indexed.
			// We should have deleted it using the "previous routes" (if it was actually indexed previously).
			return deletePreviousFuture;
		}
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				currentRoute.routingKey(), identifier );
		return deletePreviousFuture.thenCombine( delegate.delete( referenceProvider, commitStrategy, refreshStrategy ),
				(deletePreviousResult, deleteResult) -> deleteResult );
	}

	CompletableFuture<?> purge(Object providedId, String providedRoutingKey,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		I identifier = typeContext.identifierMapping().getIdentifier( providedId );
		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				providedRoutingKey, identifier );
		return delegate.delete( referenceProvider, commitStrategy, refreshStrategy );
	}

	private CompletableFuture<?> deletePrevious(String documentIdentifier, List<DocumentRouteImpl> previousRoutes,
			I identifier, DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		if ( previousRoutes.isEmpty() ) {
			return CompletableFuture.completedFuture( null );
		}
		CompletableFuture<?>[] futures = new CompletableFuture[previousRoutes.size()];
		for ( int i = 0; i < previousRoutes.size(); i++ ) {
			DocumentRouteImpl route = previousRoutes.get( i );
			DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
					route.routingKey(), identifier );
			futures[i] = delegate.delete( referenceProvider, commitStrategy, refreshStrategy );
		}
		return CompletableFuture.allOf( futures );
	}
}
