/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.search.predicate.impl.SearchPredicateFactoryImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionFactoryImpl;
import org.hibernate.search.backend.lucene.search.sort.impl.SearchSortFactoryImpl;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;

/**
 * @author Guillaume Smet
 */
public class LuceneSearchTargetContext
		implements SearchTargetContext<LuceneSearchQueryElementCollector> {

	private final SearchPredicateFactoryImpl searchPredicateFactory;
	private final SearchSortFactoryImpl searchSortFactory;
	private final SearchQueryFactoryImpl searchQueryFactory;
	private final LuceneSearchProjectionFactoryImpl searchProjectionFactory;

	public LuceneSearchTargetContext(SearchBackendContext searchBackendContext, LuceneSearchTargetModel searchTargetModel) {
		this.searchPredicateFactory = new SearchPredicateFactoryImpl( searchTargetModel );
		this.searchSortFactory = new SearchSortFactoryImpl( searchTargetModel );
		this.searchProjectionFactory = new LuceneSearchProjectionFactoryImpl( searchTargetModel );
		this.searchQueryFactory = new SearchQueryFactoryImpl( searchBackendContext, searchTargetModel, this.searchProjectionFactory );
	}

	@Override
	public SearchPredicateFactoryImpl getSearchPredicateFactory() {
		return searchPredicateFactory;
	}

	@Override
	public SearchSortFactoryImpl getSearchSortFactory() {
		return searchSortFactory;
	}

	@Override
	public SearchQueryFactoryImpl getSearchQueryFactory() {
		return searchQueryFactory;
	}

	@Override
	public LuceneSearchProjectionFactoryImpl getSearchProjectionFactory() {
		return searchProjectionFactory;
	}
}
