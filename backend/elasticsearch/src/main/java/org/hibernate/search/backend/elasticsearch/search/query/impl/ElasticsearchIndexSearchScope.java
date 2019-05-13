/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchScopeModel;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactoryImpl;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;

public class ElasticsearchIndexSearchScope
		implements IndexSearchScope<ElasticsearchSearchQueryElementCollector> {

	private final ElasticsearchSearchScopeModel model;
	private final ElasticsearchSearchPredicateBuilderFactoryImpl searchPredicateFactory;
	private final ElasticsearchSearchSortBuilderFactoryImpl searchSortFactory;
	private final ElasticsearchSearchQueryBuilderFactory searchQueryFactory;
	private final ElasticsearchSearchProjectionBuilderFactory searchProjectionFactory;

	public ElasticsearchIndexSearchScope(
			MappingContextImplementor mappingContext,
			SearchBackendContext searchBackendContext,
			ElasticsearchSearchScopeModel model) {
		ElasticsearchSearchContext searchContext = searchBackendContext.createSearchContext(
				mappingContext, model
		);
		this.model = model;
		this.searchPredicateFactory = new ElasticsearchSearchPredicateBuilderFactoryImpl( searchContext, model );
		this.searchSortFactory = new ElasticsearchSearchSortBuilderFactoryImpl( searchContext, model );
		this.searchProjectionFactory = new ElasticsearchSearchProjectionBuilderFactory(
				searchBackendContext.getSearchProjectionBackendContext(),
				model
		);
		this.searchQueryFactory = new ElasticsearchSearchQueryBuilderFactory(
				searchBackendContext, searchContext,
				this.searchProjectionFactory
		);
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( model.getHibernateSearchIndexNames() )
				.append( "]" )
				.toString();
	}

	@Override
	public ElasticsearchSearchPredicateBuilderFactoryImpl getSearchPredicateBuilderFactory() {
		return searchPredicateFactory;
	}

	@Override
	public ElasticsearchSearchSortBuilderFactoryImpl getSearchSortBuilderFactory() {
		return searchSortFactory;
	}

	@Override
	public ElasticsearchSearchQueryBuilderFactory getSearchQueryBuilderFactory() {
		return searchQueryFactory;
	}

	@Override
	public ElasticsearchSearchProjectionBuilderFactory getSearchProjectionFactory() {
		return searchProjectionFactory;
	}
}
