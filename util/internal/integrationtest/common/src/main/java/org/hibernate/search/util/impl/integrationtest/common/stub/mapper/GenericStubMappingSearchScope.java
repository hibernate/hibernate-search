/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.loading.spi.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

/**
 * A wrapper around {@link MappedIndexSearchScope} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public class GenericStubMappingSearchScope<R, O> {

	private final MappedIndexSearchScope<R, O> delegate;
	private final Function<DocumentReference, R> documentReferenceTransformer;

	GenericStubMappingSearchScope(MappedIndexSearchScope<R, O> delegate,
			Function<DocumentReference, R> documentReferenceTransformer) {
		this.delegate = delegate;
		this.documentReferenceTransformer = documentReferenceTransformer;
	}

	public StubMappingQueryResultDefinitionContext<R, O> query(ObjectLoader<R, O> objectLoader) {
		return query( new StubSessionContext(), objectLoader );
	}

	public StubMappingQueryResultDefinitionContext<R, O> query(StubSessionContext sessionContext,
			ObjectLoader<R, O> objectLoader) {
		LoadingContextBuilder<R, O> loadingContextBuilder = () -> new StubLoadingContext<>(
				new DefaultProjectionHitMapper<>( documentReferenceTransformer, objectLoader )
		);
		return new StubMappingQueryResultDefinitionContext<>( delegate, sessionContext, loadingContextBuilder );
	}

	public SearchPredicateFactoryContext predicate() {
		return delegate.predicate();
	}

	public SearchSortContainerContext sort() {
		return delegate.sort();
	}

	public SearchProjectionFactoryContext<R, O> projection() {
		return delegate.projection();
	}
}
