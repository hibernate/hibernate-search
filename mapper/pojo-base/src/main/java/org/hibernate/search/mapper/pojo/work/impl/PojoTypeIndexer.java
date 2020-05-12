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

	CompletableFuture<?> add(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider(
				sessionContext, identifier, entitySupplier
		);
		// FIXME HSEARCH-3902 allow configuring the commit/refresh strategy
		return delegate.add( referenceProvider, typeContext.toDocumentContributor( entitySupplier, sessionContext ),
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
	}

	CompletableFuture<?> addOrUpdate(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider(
				sessionContext, identifier, entitySupplier
		);
		// FIXME HSEARCH-3902 allow configuring the commit/refresh strategy
		return delegate.update( referenceProvider, typeContext.toDocumentContributor( entitySupplier, sessionContext ),
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
	}

	CompletableFuture<?> delete(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider(
				sessionContext, identifier, entitySupplier
		);
		// FIXME HSEARCH-3902 allow configuring the commit/refresh strategy
		return delegate.delete( referenceProvider, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
	}

	CompletableFuture<?> purge(Object providedId, String providedRoutingKey) {
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId );
		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider(
				sessionContext, identifier, providedRoutingKey
		);
		// FIXME HSEARCH-3902 allow configuring the commit/refresh strategy
		return delegate.delete( referenceProvider, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
	}
}
