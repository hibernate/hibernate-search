/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.scope.impl;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.impl.DefaultSearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;

class MappedIndexScopeImpl<R, E> implements MappedIndexScope<R, E> {

	private final IndexScope delegate;

	MappedIndexScopeImpl(IndexScope delegate) {
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegate=" + delegate + "]";
	}

	@Override
	public <LOS> SearchQuerySelectStep<?, R, E, LOS, SearchProjectionFactory<R, E>, ?> search(
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<E, LOS> loadingContextBuilder) {
		return new DefaultSearchQuerySelectStep<>( delegate.searchScope(), sessionContext, loadingContextBuilder );
	}

	@Override
	public SearchPredicateFactory predicate() {
		return delegate.searchScope().predicateFactory();
	}

	@Override
	public SearchSortFactory sort() {
		return delegate.searchScope().sortFactory();
	}

	@Override
	public SearchProjectionFactory<R, E> projection() {
		return delegate.searchScope().projectionFactory();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return delegate.searchScope().aggregationFactory();
	}

	@Override
	public SearchHighlighterFactory highlighter() {
		return delegate.searchScope().highlighterFactory();
	}

	@Override
	public <T> T extension(IndexScopeExtension<T> extension) {
		return delegate.extension( extension );
	}
}
