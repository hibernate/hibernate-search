/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateCollector;
import org.hibernate.search.backend.lucene.search.predicate.impl.SearchPredicateFactoryImpl;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.search.sort.impl.SearchSortFactoryImpl;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

/**
 * @author Guillaume Smet
 */
public class LuceneSearchTargetContext
		implements SearchTargetContext<LuceneSearchQueryElementCollector> {

	private final SearchPredicateFactory<LuceneSearchPredicateCollector> searchPredicateFactory;
	private final SearchSortFactory<LuceneSearchSortCollector> searchSortFactory;
	private final SearchQueryFactory<LuceneSearchQueryElementCollector> searchQueryFactory;

	public LuceneSearchTargetContext(SearchBackendContext searchBackendContext, LuceneSearchTargetModel searchTargetModel) {
		this.searchPredicateFactory = new SearchPredicateFactoryImpl( searchTargetModel );
		this.searchSortFactory = new SearchSortFactoryImpl( searchTargetModel );
		this.searchQueryFactory = new SearchQueryFactoryImpl( searchBackendContext, searchTargetModel );
	}

	@Override
	public SearchPredicateFactory<LuceneSearchPredicateCollector> getSearchPredicateFactory() {
		return searchPredicateFactory;
	}

	@Override
	public SearchSortFactory<LuceneSearchSortCollector> getSearchSortFactory() {
		return searchSortFactory;
	}

	@Override
	public SearchQueryFactory<LuceneSearchQueryElementCollector> getSearchQueryFactory() {
		return searchQueryFactory;
	}
}
