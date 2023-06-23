/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

class ElasticsearchSearchQueryOptionsStepImpl<H, LOS>
		extends AbstractExtendedSearchQueryOptionsStep<
				ElasticsearchSearchQueryOptionsStep<H, LOS>,
				H,
				ElasticsearchSearchResult<H>,
				ElasticsearchSearchScroll<H>,
				LOS,
				ElasticsearchSearchPredicateFactory,
				ElasticsearchSearchSortFactory,
				ElasticsearchSearchAggregationFactory,
				ElasticsearchSearchQueryIndexScope<?>>
		implements ElasticsearchSearchQueryWhereStep<H, LOS>, ElasticsearchSearchQueryOptionsStep<H, LOS> {

	private final ElasticsearchSearchQueryBuilder<H> searchQueryBuilder;

	ElasticsearchSearchQueryOptionsStepImpl(ElasticsearchSearchQueryIndexScope<?> scope,
			ElasticsearchSearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, LOS> loadingContextBuilder) {
		super( scope, searchQueryBuilder, loadingContextBuilder );
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public ElasticsearchSearchQueryOptionsStep<H, LOS> requestTransformer(ElasticsearchSearchRequestTransformer transformer) {
		searchQueryBuilder.requestTransformer( transformer );
		return thisAsS();
	}

	@Override
	public ElasticsearchSearchQuery<H> toQuery() {
		return searchQueryBuilder.build();
	}

	@Override
	protected ElasticsearchSearchQueryOptionsStepImpl<H, LOS> thisAsS() {
		return this;
	}

	@Override
	protected ElasticsearchSearchPredicateFactory predicateFactory() {
		return scope.predicateFactory();
	}

	@Override
	protected ElasticsearchSearchSortFactory sortFactory() {
		return scope.sortFactory();
	}

	@Override
	protected ElasticsearchSearchAggregationFactory aggregationFactory() {
		return scope.aggregationFactory();
	}

	@Override
	protected SearchHighlighterFactory highlighterFactory() {
		return scope.highlighterFactory();
	}
}
