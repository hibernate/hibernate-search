/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.ElasticsearchSearchAggregationFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScroll;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryOptionsStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryWhereStep;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryIndexScope;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractExtendedSearchQueryOptionsStep;

class ElasticsearchSearchQueryOptionsStepImpl<SR, H, LOS>
		extends AbstractExtendedSearchQueryOptionsStep<
				SR,
				ElasticsearchSearchQueryOptionsStep<SR, H, LOS>,
				H,
				ElasticsearchSearchResult<H>,
				ElasticsearchSearchScroll<H>,
				LOS,
				ElasticsearchSearchPredicateFactory<SR>,
				ElasticsearchSearchSortFactory<SR>,
				ElasticsearchSearchAggregationFactory<SR>,
				ElasticsearchSearchQueryIndexScope<?>>
		implements ElasticsearchSearchQueryWhereStep<SR, H, LOS>, ElasticsearchSearchQueryOptionsStep<SR, H, LOS> {

	private final ElasticsearchSearchQueryBuilder<H> searchQueryBuilder;

	ElasticsearchSearchQueryOptionsStepImpl(ElasticsearchSearchQueryIndexScope<?> scope,
			ElasticsearchSearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, LOS> loadingContextBuilder) {
		super( scope, searchQueryBuilder, loadingContextBuilder );
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public ElasticsearchSearchQueryOptionsStep<SR, H, LOS> requestTransformer(
			ElasticsearchSearchRequestTransformer transformer) {
		searchQueryBuilder.requestTransformer( transformer );
		return thisAsS();
	}

	@Override
	public ElasticsearchSearchQuery<H> toQuery() {
		return searchQueryBuilder.build();
	}

	@Override
	protected ElasticsearchSearchQueryOptionsStepImpl<SR, H, LOS> thisAsS() {
		return this;
	}

	@Override
	protected ElasticsearchSearchPredicateFactory<SR> predicateFactory() {
		return scope.predicateFactory();
	}

	@Override
	protected ElasticsearchSearchSortFactory<SR> sortFactory() {
		return scope.sortFactory();
	}

	@Override
	protected ElasticsearchSearchAggregationFactory<SR> aggregationFactory() {
		return scope.aggregationFactory();
	}

	@Override
	protected SearchHighlighterFactory highlighterFactory() {
		return scope.highlighterFactory();
	}
}
