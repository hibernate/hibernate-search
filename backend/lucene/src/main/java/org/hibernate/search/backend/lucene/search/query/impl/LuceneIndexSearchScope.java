/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchScopeModel;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactoryImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilderFactoryImpl;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;

/**
 * @author Guillaume Smet
 */
public class LuceneIndexSearchScope
		implements IndexSearchScope<LuceneSearchQueryElementCollector> {

	private final LuceneSearchScopeModel model;
	private final LuceneSearchPredicateBuilderFactoryImpl searchPredicateFactory;
	private final LuceneSearchSortBuilderFactoryImpl searchSortFactory;
	private final LuceneSearchQueryBuilderFactory searchQueryFactory;
	private final LuceneSearchProjectionBuilderFactory searchProjectionFactory;

	public LuceneIndexSearchScope(SearchBackendContext searchBackendContext,
			MappingContextImplementor mappingContext,
			LuceneSearchScopeModel model) {
		LuceneSearchContext searchContext = searchBackendContext.createSearchContext( mappingContext, model );
		this.model = model;
		this.searchPredicateFactory = new LuceneSearchPredicateBuilderFactoryImpl( searchContext, model );
		this.searchSortFactory = new LuceneSearchSortBuilderFactoryImpl( searchContext, model );
		this.searchProjectionFactory = new LuceneSearchProjectionBuilderFactory( model );
		this.searchQueryFactory = new LuceneSearchQueryBuilderFactory( searchBackendContext, searchContext, this.searchProjectionFactory );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( model.getIndexNames() )
				.append( "]" )
				.toString();
	}

	@Override
	public LuceneSearchPredicateBuilderFactoryImpl getSearchPredicateBuilderFactory() {
		return searchPredicateFactory;
	}

	@Override
	public LuceneSearchSortBuilderFactoryImpl getSearchSortBuilderFactory() {
		return searchSortFactory;
	}

	@Override
	public LuceneSearchQueryBuilderFactory getSearchQueryBuilderFactory() {
		return searchQueryFactory;
	}

	@Override
	public LuceneSearchProjectionBuilderFactory getSearchProjectionFactory() {
		return searchProjectionFactory;
	}
}
