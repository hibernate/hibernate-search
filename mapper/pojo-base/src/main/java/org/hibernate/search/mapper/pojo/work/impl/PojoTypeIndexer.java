/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
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
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId, entitySupplier );

		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider(
				sessionContext, identifier, providedRoutingKey, entitySupplier
		);
		return delegate.add( referenceProvider, typeContext.toDocumentContributor( entitySupplier, sessionContext ),
				commitStrategy, refreshStrategy );
	}

	CompletableFuture<?> addOrUpdate(Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider(
				sessionContext, identifier, providedRoutingKey, entitySupplier
		);
		return delegate.update( referenceProvider, typeContext.toDocumentContributor( entitySupplier, sessionContext ),
				commitStrategy, refreshStrategy );
	}

	CompletableFuture<?> delete(Object providedId, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider(
				// TODO HSEARCH-3891 expose the providedRoutingKey
				sessionContext, identifier, null, entitySupplier
		);
		return delegate.delete( referenceProvider, commitStrategy, refreshStrategy );
	}

	CompletableFuture<?> purge(Object providedId, String providedRoutingKey,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId );
		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider(
				sessionContext, identifier, providedRoutingKey
		);
		return delegate.delete( referenceProvider, commitStrategy, refreshStrategy );
	}
}
