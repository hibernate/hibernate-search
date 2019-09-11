/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

/**
 * A wrapper around {@link MappedIndexScope} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public class GenericStubMappingScope<R, E> {

	private final MappedIndexScope<R, E> delegate;

	GenericStubMappingScope(MappedIndexScope<R, E> delegate) {
		this.delegate = delegate;
	}

	public SearchQueryHitTypeStep<?, R, E, ?, ?> query(LoadingContext<R, E> loadingContext) {
		return query( new StubSessionContext(), loadingContext );
	}

	public SearchQueryHitTypeStep<?, R, E, ?, ?> query(StubSessionContext sessionContext,
			LoadingContext<R, E> loadingContext) {
		LoadingContextBuilder<R, E> loadingContextBuilder = () -> loadingContext;
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
}
