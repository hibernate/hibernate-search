/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.search.predicate.impl.SearchPredicateBuilderFactoryImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactoryImpl;
import org.hibernate.search.backend.lucene.search.sort.impl.SearchSortBuilderFactoryImpl;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;

/**
 * @author Guillaume Smet
 */
public class LuceneSearchTargetContext
		implements SearchTargetContext<LuceneSearchQueryElementCollector> {

	private final SearchPredicateBuilderFactoryImpl searchPredicateFactory;
	private final SearchSortBuilderFactoryImpl searchSortFactory;
	private final SearchQueryBuilderFactoryImpl searchQueryFactory;
	private final LuceneSearchProjectionBuilderFactoryImpl searchProjectionFactory;

	public LuceneSearchTargetContext(SearchBackendContext searchBackendContext, LuceneSearchTargetModel searchTargetModel) {
		this.searchPredicateFactory = new SearchPredicateBuilderFactoryImpl( searchTargetModel );
		this.searchSortFactory = new SearchSortBuilderFactoryImpl( searchTargetModel );
		this.searchProjectionFactory = new LuceneSearchProjectionBuilderFactoryImpl( searchTargetModel );
		this.searchQueryFactory = new SearchQueryBuilderFactoryImpl( searchBackendContext, searchTargetModel, this.searchProjectionFactory );
	}

	@Override
	public SearchPredicateBuilderFactoryImpl getSearchPredicateBuilderFactory() {
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
