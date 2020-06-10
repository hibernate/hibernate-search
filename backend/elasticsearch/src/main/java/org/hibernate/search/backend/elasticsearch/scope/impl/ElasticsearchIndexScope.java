/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactoryImpl;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;

public class ElasticsearchIndexScope
		implements IndexScope<ElasticsearchSearchQueryElementCollector> {

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchSearchPredicateBuilderFactoryImpl searchPredicateFactory;
	private final ElasticsearchSearchSortBuilderFactoryImpl searchSortFactory;
	private final ElasticsearchSearchProjectionBuilderFactory searchProjectionFactory;
	private final ElasticsearchSearchAggregationBuilderFactory searchAggregationFactory;
	private final ElasticsearchSearchQueryBuilderFactory searchQueryFactory;

	public ElasticsearchIndexScope(
			BackendMappingContext mappingContext,
			SearchBackendContext backendContext,
			ElasticsearchSearchIndexesContext searchIndexesContext) {
		this.searchContext = backendContext.createSearchContext(
				mappingContext, searchIndexesContext
		);
		this.searchPredicateFactory = new ElasticsearchSearchPredicateBuilderFactoryImpl( searchContext );
		this.searchSortFactory = new ElasticsearchSearchSortBuilderFactoryImpl( searchContext );
		this.searchProjectionFactory = new ElasticsearchSearchProjectionBuilderFactory(
				backendContext.getSearchProjectionBackendContext(), searchContext
		);
		this.searchAggregationFactory = new ElasticsearchSearchAggregationBuilderFactory( searchContext );
		this.searchQueryFactory = new ElasticsearchSearchQueryBuilderFactory(
				backendContext, searchContext,
				this.searchProjectionFactory
		);
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( searchContext.indexes().hibernateSearchIndexNames() )
				.append( "]" )
				.toString();
	}

	@Override
	public ElasticsearchSearchPredicateBuilderFactoryImpl searchPredicateBuilderFactory() {
		return searchPredicateFactory;
	}

	@Override
	public ElasticsearchSearchSortBuilderFactoryImpl searchSortBuilderFactory() {
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
	public SearchAggregationBuilderFactory<? super ElasticsearchSearchQueryElementCollector> searchAggregationFactory() {
		return searchAggregationFactory;
	}
}
