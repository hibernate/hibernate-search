/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public class ElasticsearchIndexScope
		implements IndexScope {

	private final ElasticsearchSearchIndexScope searchScope;
	private final ElasticsearchSearchPredicateBuilderFactory searchPredicateFactory;
	private final ElasticsearchSearchSortBuilderFactory searchSortFactory;
	private final ElasticsearchSearchProjectionBuilderFactory searchProjectionFactory;
	private final ElasticsearchSearchAggregationBuilderFactory searchAggregationFactory;
	private final ElasticsearchSearchQueryBuilderFactory searchQueryFactory;

	public ElasticsearchIndexScope(BackendMappingContext mappingContext, SearchBackendContext backendContext,
			Set<ElasticsearchIndexModel> indexModels) {
		this.searchScope = backendContext.createSearchContext( mappingContext, indexModels );
		this.searchPredicateFactory = new ElasticsearchSearchPredicateBuilderFactory( searchScope );
		this.searchSortFactory = new ElasticsearchSearchSortBuilderFactory( searchScope );
		this.searchProjectionFactory = new ElasticsearchSearchProjectionBuilderFactory(
				backendContext.getSearchProjectionBackendContext(), searchScope
		);
		this.searchAggregationFactory = new ElasticsearchSearchAggregationBuilderFactory( searchScope );
		this.searchQueryFactory = new ElasticsearchSearchQueryBuilderFactory(
				backendContext, searchScope
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[indexNames=" + searchScope.hibernateSearchIndexNames() + "]";
	}

	@Override
	public SearchIndexScope searchScope() {
		return searchScope;
	}

	@Override
	public ElasticsearchSearchPredicateBuilderFactory searchPredicateBuilderFactory() {
		return searchPredicateFactory;
	}

	@Override
	public ElasticsearchSearchSortBuilderFactory searchSortBuilderFactory() {
		return searchSortFactory;
	}

	@Override
	public ElasticsearchSearchQueryBuilderFactory searchQueryBuilderFactory() {
		return searchQueryFactory;
	}

	@Override
	public ElasticsearchSearchProjectionBuilderFactory searchProjectionFactory() {
		return searchProjectionFactory;
	}

	@Override
	public SearchAggregationBuilderFactory searchAggregationFactory() {
		return searchAggregationFactory;
	}
}
