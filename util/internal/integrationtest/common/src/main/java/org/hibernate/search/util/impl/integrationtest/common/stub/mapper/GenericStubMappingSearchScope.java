/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

/**
 * A wrapper around {@link MappedIndexSearchScope} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public class GenericStubMappingSearchScope<R, E> {

	private final MappedIndexSearchScope<R, E> delegate;

	GenericStubMappingSearchScope(MappedIndexSearchScope<R, E> delegate) {
		this.delegate = delegate;
	}

	public SearchQueryResultDefinitionContext<?, R, E, ?, ?> query(LoadingContext<R, E> loadingContext) {
		return query( new StubSessionContext(), loadingContext );
	}

	public SearchQueryResultDefinitionContext<?, R, E, ?, ?> query(StubSessionContext sessionContext,
			LoadingContext<R, E> loadingContext) {
		LoadingContextBuilder<R, E> loadingContextBuilder = () -> loadingContext;
		return delegate.search( sessionContext, loadingContextBuilder );
	}

	public SearchPredicateFactoryContext predicate() {
		return delegate.predicate();
	}

	public SearchSortContainerContext sort() {
		return delegate.sort();
	}

	public SearchProjectionFactoryContext<R, E> projection() {
		return delegate.projection();
	}
}
