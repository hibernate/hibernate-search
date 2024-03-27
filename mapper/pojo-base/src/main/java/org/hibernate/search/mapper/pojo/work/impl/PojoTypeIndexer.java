/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.NoOpDocumentRouter;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

public class PojoTypeIndexer<I, E> implements PojoIndexingProcessorRootContext {

	private final PojoWorkSessionContext sessionContext;
	private final PojoWorkIndexedTypeContext<I, E> typeContext;
	private final IndexIndexer delegate;

	public PojoTypeIndexer(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext,
			IndexIndexer delegate) {
		this.sessionContext = sessionContext;
		this.typeContext = typeContext;
		this.delegate = delegate;
	}

	@Override
	public PojoIndexingProcessorSessionContext sessionContext() {
		return sessionContext;
	}

	@Override
	public boolean isDeleted(Object unproxiedObject) {
		// No context holding any information about deleted entities here.
		return false;
	}

	CompletableFuture<?> add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );

		DocumentRouteDescriptor currentRoute = typeContext.router()
				.currentRoute( identifier, entitySupplier, providedRoutes, sessionContext );
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
				typeContext.toDocumentContributor( sessionContext, this, identifier, entitySupplier ),
				commitStrategy, refreshStrategy, operationSubmitter
		);
	}

	CompletableFuture<?> addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );

		DocumentRoutesDescriptor routes = typeContext.router()
				.routes( identifier, entitySupplier, providedRoutes, sessionContext );

		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );

		CompletableFuture<?> deletePreviousFuture = deletePrevious( documentIdentifier, routes.previousRoutes(),
				identifier, commitStrategy, refreshStrategy, operationSubmitter );

		if ( routes.currentRoute() == null ) {
			// The routing bridge decided the entity should not be indexed.
			// We should have deleted it using the "previous routes" (if it was actually indexed previously),
			// and we don't have anything else to do.
			return deletePreviousFuture;
		}
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				routes.currentRoute().routingKey(), identifier );
		// Deletion on previous routes and update on current route can happen in parallel:
		// the backend is responsible for preserving relative order of works on the same index/shard + docId,
		// and we don't care about relative order of works on different indexes/shards.
		return deletePreviousFuture.thenCombine(
				delegate.addOrUpdate( referenceProvider,
						typeContext.toDocumentContributor( sessionContext, this, identifier, entitySupplier ),
						commitStrategy, refreshStrategy, operationSubmitter ),
				(deletePreviousResult, updateResult) -> updateResult );
	}

	CompletableFuture<?> delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, entitySupplier );

		DocumentRoutesDescriptor routes = typeContext.router()
				.routes( identifier, entitySupplier, providedRoutes, sessionContext );

		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );

		CompletableFuture<?> deletePreviousFuture = deletePrevious( documentIdentifier, routes.previousRoutes(),
				identifier, commitStrategy, refreshStrategy, operationSubmitter );

		if ( routes.currentRoute() == null ) {
			// The routing bridge decided the entity should not be indexed.
			// We should have deleted it using the "previous routes" (if it was actually indexed previously).
			return deletePreviousFuture;
		}
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				routes.currentRoute().routingKey(), identifier );
		return deletePreviousFuture.thenCombine(
				delegate.delete( referenceProvider, commitStrategy, refreshStrategy, operationSubmitter ),
				(deletePreviousResult, deleteResult) -> deleteResult );
	}

	CompletableFuture<?> delete(Object providedId, DocumentRoutesDescriptor providedRoutes,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		I identifier = typeContext.identifierMapping().getIdentifier( providedId, null );

		// Purge: entity is not available and we can't route according to its state.
		// We can use the provided routing keys, though, which is what the no-op router does.
		DocumentRoutesDescriptor routes = NoOpDocumentRouter.INSTANCE
				.routes( identifier, null, providedRoutes, sessionContext );
		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );

		CompletableFuture<?> deletePreviousFuture = deletePrevious( documentIdentifier, routes.previousRoutes(),
				identifier, commitStrategy, refreshStrategy, operationSubmitter );

		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				routes.currentRoute().routingKey(), identifier );
		return deletePreviousFuture.thenCombine(
				delegate.delete( referenceProvider, commitStrategy, refreshStrategy, operationSubmitter ),
				(deletePreviousResult, deleteResult) -> deleteResult );
	}

	private CompletableFuture<?> deletePrevious(String documentIdentifier, Collection<DocumentRouteDescriptor> previousRoutes,
			I identifier, DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		if ( previousRoutes.isEmpty() ) {
			return CompletableFuture.completedFuture( null );
		}
		CompletableFuture<?>[] futures = new CompletableFuture[previousRoutes.size()];
		int i = 0;
		for ( DocumentRouteDescriptor route : previousRoutes ) {
			DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
					route.routingKey(), identifier );
			futures[i++] = delegate.delete( referenceProvider, commitStrategy, refreshStrategy, operationSubmitter );
		}
		return CompletableFuture.allOf( futures );
	}
}
