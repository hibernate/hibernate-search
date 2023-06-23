/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;

final class DefaultSearchQueryOptionsStep<H, LOS>
		extends AbstractSearchQueryOptionsStep<
				DefaultSearchQueryOptionsStep<H, LOS>,
				H,
				LOS,
				SearchPredicateFactory,
				SearchSortFactory,
				SearchAggregationFactory,
				SearchQueryIndexScope<?>>
		implements SearchQueryWhereStep<DefaultSearchQueryOptionsStep<H, LOS>, H, LOS, SearchPredicateFactory>,
		SearchQueryOptionsStep<DefaultSearchQueryOptionsStep<H, LOS>, H, LOS, SearchSortFactory, SearchAggregationFactory> {

	DefaultSearchQueryOptionsStep(SearchQueryIndexScope<?> scope, SearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, LOS> loadingContextBuilder) {
		super( scope, searchQueryBuilder, loadingContextBuilder );
	}

	@Override
	protected SearchPredicateFactory predicateFactory() {
		return scope.predicateFactory();
	}

	@Override
	protected SearchSortFactory sortFactory() {
		return scope.sortFactory();
	}

	@Override
	protected SearchAggregationFactory aggregationFactory() {
		return scope.aggregationFactory();
	}

	@Override
	protected SearchHighlighterFactory highlighterFactory() {
		return scope.highlighterFactory();
	}

	@Override
	protected DefaultSearchQueryOptionsStep<H, LOS> thisAsS() {
		return this;
	}
}
