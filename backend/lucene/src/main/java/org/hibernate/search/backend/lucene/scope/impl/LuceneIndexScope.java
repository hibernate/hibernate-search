/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactoryImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilderFactory;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilderFactoryImpl;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;


public class LuceneIndexScope
		implements IndexScope<LuceneSearchQueryElementCollector> {

	private final LuceneScopeModel model;
	private final LuceneSearchPredicateBuilderFactoryImpl searchPredicateFactory;
	private final LuceneSearchSortBuilderFactoryImpl searchSortFactory;
	private final LuceneSearchQueryBuilderFactory searchQueryFactory;
	private final LuceneSearchProjectionBuilderFactory searchProjectionFactory;

	public LuceneIndexScope(SearchBackendContext backendContext,
			MappingContextImplementor mappingContext,
			LuceneScopeModel model) {
		this.model = model;
		LuceneSearchContext searchContext = backendContext.createSearchContext( mappingContext, model );
		this.searchPredicateFactory = new LuceneSearchPredicateBuilderFactoryImpl( searchContext, model );
		this.searchSortFactory = new LuceneSearchSortBuilderFactoryImpl( searchContext, model );
		this.searchProjectionFactory = new LuceneSearchProjectionBuilderFactory( model );
		this.searchQueryFactory = new LuceneSearchQueryBuilderFactory( backendContext, searchContext, this.searchProjectionFactory );
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
