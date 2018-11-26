/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.SearchSortBuilderFactoryImpl;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;

public class ElasticsearchSearchTargetContext
		implements SearchTargetContext<ElasticsearchSearchQueryElementCollector> {

	private final ElasticsearchSearchTargetModel searchTargetModel;
	private final ElasticsearchSearchPredicateBuilderFactoryImpl searchPredicateFactory;
	private final SearchSortBuilderFactoryImpl searchSortFactory;
	private final SearchQueryBuilderFactoryImpl searchQueryFactory;
	private final ElasticsearchSearchProjectionBuilderFactoryImpl searchProjectionFactory;

	public ElasticsearchSearchTargetContext(
			MappingContextImplementor mappingContext,
			SearchBackendContext searchBackendContext,
			ElasticsearchSearchTargetModel searchTargetModel) {
		ElasticsearchSearchContext searchContext = new ElasticsearchSearchContext( mappingContext );
		this.searchTargetModel = searchTargetModel;
		this.searchPredicateFactory = new ElasticsearchSearchPredicateBuilderFactoryImpl( searchContext, searchTargetModel );
		this.searchSortFactory = new SearchSortBuilderFactoryImpl( searchContext, searchTargetModel );
		this.searchProjectionFactory = new ElasticsearchSearchProjectionBuilderFactoryImpl(
				searchBackendContext.getSearchProjectionBackendContext(),
				searchTargetModel );
		this.searchQueryFactory = new SearchQueryBuilderFactoryImpl( searchBackendContext, searchTargetModel,
				this.searchProjectionFactory );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( searchTargetModel.getHibernateSearchIndexNames() )
				.append( "]" )
				.toString();
	}

	@Override
	public ElasticsearchSearchPredicateBuilderFactoryImpl getSearchPredicateBuilderFactory() {
		return searchPredicateFactory;
	}

	@Override
	public SearchSortBuilderFactoryImpl getSearchSortBuilderFactory() {
		return searchSortFactory;
	}

	@Override
	public SearchQueryBuilderFactoryImpl getSearchQueryBuilderFactory() {
		return searchQueryFactory;
	}

	@Override
	public ElasticsearchSearchProjectionBuilderFactoryImpl getSearchProjectionFactory() {
		return searchProjectionFactory;
	}
}
