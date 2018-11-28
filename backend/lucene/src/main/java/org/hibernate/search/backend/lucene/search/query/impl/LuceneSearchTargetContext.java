/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactoryImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactoryImpl;
import org.hibernate.search.backend.lucene.search.sort.impl.SearchSortBuilderFactoryImpl;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;

/**
 * @author Guillaume Smet
 */
public class LuceneSearchTargetContext
		implements SearchTargetContext<LuceneSearchQueryElementCollector> {

	private final LuceneSearchTargetModel searchTargetModel;
	private final LuceneSearchPredicateBuilderFactoryImpl searchPredicateFactory;
	private final SearchSortBuilderFactoryImpl searchSortFactory;
	private final SearchQueryBuilderFactoryImpl searchQueryFactory;
	private final LuceneSearchProjectionBuilderFactoryImpl searchProjectionFactory;

	public LuceneSearchTargetContext(SearchBackendContext searchBackendContext,
			MappingContextImplementor mappingContext,
			LuceneSearchTargetModel searchTargetModel) {
		LuceneSearchContext searchContext = new LuceneSearchContext( mappingContext );
		this.searchTargetModel = searchTargetModel;
		this.searchPredicateFactory = new LuceneSearchPredicateBuilderFactoryImpl( searchContext, searchTargetModel );
		this.searchSortFactory = new SearchSortBuilderFactoryImpl( searchContext, searchTargetModel );
		this.searchProjectionFactory = new LuceneSearchProjectionBuilderFactoryImpl( searchTargetModel );
		this.searchQueryFactory = new SearchQueryBuilderFactoryImpl( searchBackendContext, searchTargetModel, this.searchProjectionFactory );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( searchTargetModel.getIndexNames() )
				.append( "]" )
				.toString();
	}

	@Override
	public LuceneSearchPredicateBuilderFactoryImpl getSearchPredicateBuilderFactory() {
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
	public LuceneSearchProjectionBuilderFactoryImpl getSearchProjectionFactory() {
		return searchProjectionFactory;
	}
}
