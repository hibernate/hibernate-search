/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

public class LuceneSearchSortBuilderFactory implements SearchSortBuilderFactory {

	private final LuceneSearchIndexScope<?> scope;

	public LuceneSearchSortBuilderFactory(LuceneSearchIndexScope<?> scope) {
		this.scope = scope;
	}

	@Override
	public ScoreSortBuilder score() {
		return new LuceneScoreSort.Builder( scope );
	}

	@Override
	public SearchSort indexOrder() {
		return new LuceneIndexOrderSort( scope );
	}

	@Override
	public CompositeSortBuilder composite() {
		return new LuceneCompositeSort.Builder( scope );
	}

	public LuceneSearchSort fromLuceneSortField(SortField luceneSortField) {
		return new LuceneUserProvidedLuceneSortFieldSort( scope, luceneSortField );
	}

	public LuceneSearchSort fromLuceneSort(Sort luceneSort) {
		return new LuceneUserProvidedLuceneSortSort( scope, luceneSort );
	}
}
