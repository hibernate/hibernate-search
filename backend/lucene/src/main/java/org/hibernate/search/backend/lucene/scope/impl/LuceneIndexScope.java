/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.impl;

import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactoryImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilderFactory;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilderFactoryImpl;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;


public class LuceneIndexScope
		implements IndexScope<LuceneSearchQueryElementCollector> {

	private final LuceneScopeModel model;
	private final LuceneSearchPredicateBuilderFactoryImpl searchPredicateFactory;
	private final LuceneSearchSortBuilderFactoryImpl searchSortFactory;
	private final LuceneSearchQueryBuilderFactory searchQueryFactory;
	private final LuceneSearchProjectionBuilderFactory searchProjectionFactory;
	private final LuceneSearchAggregationBuilderFactory searchAggregationFactory;

	public LuceneIndexScope(SearchBackendContext backendContext,
			BackendMappingContext mappingContext,
			LuceneScopeModel model) {
		this.model = model;
		LuceneSearchContext searchContext = backendContext.createSearchContext( mappingContext, model );
		this.searchPredicateFactory = new LuceneSearchPredicateBuilderFactoryImpl( searchContext, model );
		this.searchSortFactory = new LuceneSearchSortBuilderFactoryImpl( searchContext, model );
		this.searchProjectionFactory = new LuceneSearchProjectionBuilderFactory( model );
		this.searchAggregationFactory = new LuceneSearchAggregationBuilderFactory( searchContext, model );
		this.searchQueryFactory = new LuceneSearchQueryBuilderFactory( backendContext, searchContext, this.searchProjectionFactory );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( model.indexNames() )
				.append( "]" )
				.toString();
	}

	@Override
	public LuceneSearchPredicateBuilderFactoryImpl searchPredicateBuilderFactory() {
		return searchPredicateFactory;
	}

	@Override
	public LuceneSearchSortBuilderFactoryImpl searchSortBuilderFactory() {
		return searchSortFactory;
	}

	@Override
	public LuceneSearchQueryBuilderFactory searchQueryBuilderFactory() {
		return searchQueryFactory;
	}

	@Override
	public LuceneSearchProjectionBuilderFactory searchProjectionFactory() {
		return searchProjectionFactory;
	}

	@Override
	public SearchAggregationBuilderFactory<? super LuceneSearchQueryElementCollector> searchAggregationFactory() {
		return searchAggregationFactory;
	}
}
