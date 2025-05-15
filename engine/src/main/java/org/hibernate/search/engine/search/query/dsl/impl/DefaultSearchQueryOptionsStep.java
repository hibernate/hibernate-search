/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;

final class DefaultSearchQueryOptionsStep<SR, H, LOS>
		extends AbstractSearchQueryOptionsStep<
				SR,
				DefaultSearchQueryOptionsStep<SR, H, LOS>,
				H,
				LOS,
				TypedSearchPredicateFactory<SR>,
				TypedSearchSortFactory<SR>,
				TypedSearchAggregationFactory<SR>,
				SearchQueryIndexScope<?>>
		implements SearchQueryWhereStep<SR, DefaultSearchQueryOptionsStep<SR, H, LOS>, H, LOS, TypedSearchPredicateFactory<SR>>,
		SearchQueryOptionsStep<SR,
				DefaultSearchQueryOptionsStep<SR, H, LOS>,
				H,
				LOS,
				TypedSearchSortFactory<SR>,
				TypedSearchAggregationFactory<SR>> {

	DefaultSearchQueryOptionsStep(SearchQueryIndexScope<?> scope, SearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, LOS> loadingContextBuilder) {
		super( scope, searchQueryBuilder, loadingContextBuilder );
	}

	@Override
	protected TypedSearchPredicateFactory<SR> predicateFactory() {
		return scope.predicateFactory();
	}

	@Override
	protected TypedSearchSortFactory<SR> sortFactory() {
		return scope.sortFactory();
	}

	@Override
	protected TypedSearchAggregationFactory<SR> aggregationFactory() {
		return scope.aggregationFactory();
	}

	@Override
	protected SearchHighlighterFactory highlighterFactory() {
		return scope.highlighterFactory();
	}

	@Override
	protected DefaultSearchQueryOptionsStep<SR, H, LOS> thisAsS() {
		return this;
	}
}
