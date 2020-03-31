/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.scope.impl;

import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.impl.DefaultSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.impl.SearchAggregationDslContextImpl;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.impl.DefaultSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.impl.DefaultSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.impl.DefaultSearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.impl.DefaultSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.impl.SearchSortDslContextImpl;

class MappedIndexScopeImpl<C, R, E> implements MappedIndexScope<R, E> {

	private final IndexScope<C> delegate;

	MappedIndexScopeImpl(IndexScope<C> delegate) {
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "delegate=" ).append( delegate )
				.append( "]" )
				.toString();
	}

	@Override
	public <LOS> SearchQuerySelectStep<?, R, E, LOS, SearchProjectionFactory<R, E>, ?> search(
			BackendSessionContext sessionContext,
			LoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
		return new DefaultSearchQuerySelectStep<>( delegate, sessionContext, loadingContextBuilder );
	}

	@Override
	public SearchPredicateFactory predicate() {
		return new DefaultSearchPredicateFactory<>( delegate.getSearchPredicateBuilderFactory() );
	}

	@Override
	public SearchSortFactory sort() {
		return new DefaultSearchSortFactory<>(
				SearchSortDslContextImpl.root( delegate.getSearchSortBuilderFactory(), delegate.getSearchPredicateBuilderFactory() )
		);
	}

	@Override
	public SearchProjectionFactory<R, E> projection() {
		return new DefaultSearchProjectionFactory<>( delegate.getSearchProjectionFactory() );
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return new DefaultSearchAggregationFactory(
				SearchAggregationDslContextImpl.root( delegate.getSearchAggregationFactory() )
		);
	}
}
