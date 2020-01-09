/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.impl;

import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryPredicateStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;

final class DefaultSearchQueryOptionsStep<H, LOS, C>
		extends AbstractSearchQueryOptionsStep<
						DefaultSearchQueryOptionsStep<H, LOS,C>,
						H,
						LOS,
						SearchPredicateFactory,
						SearchSortFactory,
						SearchAggregationFactory,
						C
				>
		implements SearchQueryPredicateStep<DefaultSearchQueryOptionsStep<H, LOS,C>, H, SearchPredicateFactory>,
				SearchQueryOptionsStep<DefaultSearchQueryOptionsStep<H, LOS, C>, H, LOS, SearchSortFactory, SearchAggregationFactory> {

	DefaultSearchQueryOptionsStep(IndexScope<C> indexScope, SearchQueryBuilder<H, C> searchQueryBuilder) {
		super( indexScope, searchQueryBuilder );
	}

	@Override
	protected SearchPredicateFactory extendPredicateFactory(
			SearchPredicateFactory predicateFactory) {
		// We don't extend anything.
		return predicateFactory;
	}

	@Override
	protected SearchSortFactory extendSortFactory(SearchSortFactory sortFactory) {
		// We don't extend anything.
		return sortFactory;
	}

	@Override
	protected SearchAggregationFactory extendAggregationFactory(SearchAggregationFactory aggregationFactory) {
		// We don't extend anything.
		return aggregationFactory;
	}

	@Override
	protected DefaultSearchQueryOptionsStep<H, LOS, C> thisAsS() {
		return this;
	}
}
