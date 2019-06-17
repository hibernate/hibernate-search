/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;

public class PojoTypeDocumentWorkExecutor<I, E, D extends DocumentElement> {

	private final AbstractPojoSessionContextImplementor sessionContext;
	private final PojoWorkIndexedTypeContext<I, E, D> typeContext;
	private final IndexDocumentWorkExecutor<D> delegate;

	public PojoTypeDocumentWorkExecutor(PojoWorkIndexedTypeContext<I, E, D> typeContext,
			AbstractPojoSessionContextImplementor sessionContext,
			IndexDocumentWorkExecutor<D> delegate) {
		this.sessionContext = sessionContext;
		this.typeContext = typeContext;
		this.delegate = delegate;
	}

	CompletableFuture<?> add(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		DocumentReferenceProvider referenceProvider = typeContext.toDocumentReferenceProvider( sessionContext, identifier, entitySupplier );
		return delegate.add( referenceProvider, typeContext.toDocumentContributor( entitySupplier, sessionContext ) );
	}
}
