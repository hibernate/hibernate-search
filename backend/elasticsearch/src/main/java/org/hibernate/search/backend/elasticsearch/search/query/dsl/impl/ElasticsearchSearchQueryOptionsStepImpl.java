/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.dsl.impl;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.ElasticsearchSearchAggregationFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryOptionsStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryWhereStep;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractExtendedSearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;

class ElasticsearchSearchQueryOptionsStepImpl<H, LOS>
		extends AbstractExtendedSearchQueryOptionsStep<
						ElasticsearchSearchQueryOptionsStep<H, LOS>,
						H,
						ElasticsearchSearchResult<H>,
						LOS,
						ElasticsearchSearchPredicateFactory,
						ElasticsearchSearchSortFactory,
						ElasticsearchSearchAggregationFactory,
						ElasticsearchSearchQueryElementCollector
				>
		implements ElasticsearchSearchQueryWhereStep<H, LOS>, ElasticsearchSearchQueryOptionsStep<H, LOS> {

	private final ElasticsearchSearchQueryBuilder<H> searchQueryBuilder;

	ElasticsearchSearchQueryOptionsStepImpl(ElasticsearchIndexScope indexSearchScope,
			ElasticsearchSearchQueryBuilder<H> searchQueryBuilder,
			LoadingContextBuilder<?, ?, LOS> loadingContextBuilder) {
		super( indexSearchScope, searchQueryBuilder, loadingContextBuilder );
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
	protected ElasticsearchSearchPredicateFactory extendPredicateFactory(
			SearchPredicateFactory predicateFactory) {
		return predicateFactory.extension( ElasticsearchExtension.get() );
	}

	@Override
	protected ElasticsearchSearchSortFactory extendSortFactory(
			SearchSortFactory sortFactory) {
		return sortFactory.extension( ElasticsearchExtension.get() );
	}

	@Override
	protected ElasticsearchSearchAggregationFactory extendAggregationFactory(SearchAggregationFactory aggregationFactory) {
		return aggregationFactory.extension( ElasticsearchExtension.get() );
	}
}
