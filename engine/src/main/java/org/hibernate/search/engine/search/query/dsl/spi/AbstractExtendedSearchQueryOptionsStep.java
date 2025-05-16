/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.query.ExtendedSearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;

public abstract class AbstractExtendedSearchQueryOptionsStep<
		SR,
		S extends SearchQueryOptionsStep<SR, S, H, LOS, SF, AF>,
		H,
		R extends SearchResult<H>,
		SCR extends SearchScroll<H>,
		LOS,
		PDF extends TypedSearchPredicateFactory<SR>,
		SF extends TypedSearchSortFactory<SR>,
		AF extends TypedSearchAggregationFactory<SR>,
		SC extends SearchQueryIndexScope<?>>
		extends AbstractSearchQueryOptionsStep<SR, S, H, LOS, PDF, SF, AF, SC> {

	public AbstractExtendedSearchQueryOptionsStep(SC scope,
			SearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, LOS> loadingContextBuilder) {
		super( scope, searchQueryBuilder, loadingContextBuilder );
	}

	@Override
	public abstract ExtendedSearchQuery<H, R, SCR> toQuery();

	@Override
	public R fetchAll() {
		return toQuery().fetchAll();
	}

	@Override
	public R fetch(Integer limit) {
		return toQuery().fetch( limit );
	}

	@Override
	public R fetch(Integer offset, Integer limit) {
		return toQuery().fetch( offset, limit );
	}

	@Override
	public SCR scroll(int chunkSize) {
		return toQuery().scroll( chunkSize );
	}
}
