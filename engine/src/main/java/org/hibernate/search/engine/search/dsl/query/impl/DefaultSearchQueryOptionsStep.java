/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import org.hibernate.search.engine.search.dsl.aggregation.SearchAggregationFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryPredicateStep;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

final class DefaultSearchQueryOptionsStep<H, C>
		extends AbstractSearchQueryOptionsStep<
						DefaultSearchQueryOptionsStep<H, C>,
						H,
						SearchPredicateFactory,
						SearchSortFactory,
						SearchAggregationFactory,
						C
				>
		implements SearchQueryPredicateStep<DefaultSearchQueryOptionsStep<H, C>, H, SearchPredicateFactory>,
		SearchQueryOptionsStep<DefaultSearchQueryOptionsStep<H, C>, H, SearchSortFactory, SearchAggregationFactory> {

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
	protected DefaultSearchQueryOptionsStep<H, C> thisAsS() {
		return this;
	}
}
