/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;

/**
 * A wrapper around {@link MappedIndexScope} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public class GenericStubMappingScope<R, E> {

	private final MappedIndexScope<R, E> delegate;

	GenericStubMappingScope(MappedIndexScope<R, E> delegate) {
		this.delegate = delegate;
	}

	public SearchQuerySelectStep<?, R, E, StubLoadingOptionsStep, ?, ?> query(
			SearchLoadingContext<R, E> loadingContext) {
		return query( new StubBackendSessionContext(), loadingContext );
	}

	public SearchQuerySelectStep<?, R, E, StubLoadingOptionsStep, ?, ?> query(StubBackendSessionContext sessionContext,
			SearchLoadingContext<R, E> loadingContext) {
		return query( sessionContext, loadingContext, new StubLoadingOptionsStep() );
	}

	public <LOS> SearchQuerySelectStep<?, R, E, LOS, ?, ?> query(StubBackendSessionContext sessionContext,
			SearchLoadingContext<R, E> loadingContext, LOS loadingOptionsStep) {
		SearchLoadingContextBuilder<R, E, LOS> loadingContextBuilder = new SearchLoadingContextBuilder<R, E, LOS>() {
			@Override
			public LOS toAPI() {
				return loadingOptionsStep;
			}

			@Override
			public SearchLoadingContext<R, E> build() {
				return loadingContext;
			}
		};
		return delegate.search( sessionContext, loadingContextBuilder );
	}

	public SearchPredicateFactory predicate() {
		return delegate.predicate();
	}

	public SearchSortFactory sort() {
		return delegate.sort();
	}

	public SearchProjectionFactory<R, E> projection() {
		return delegate.projection();
	}

	public SearchAggregationFactory aggregation() {
		return delegate.aggregation();
	}

	public <T> T extension(IndexScopeExtension<T> extension) {
		return delegate.extension( extension );
	}
}
