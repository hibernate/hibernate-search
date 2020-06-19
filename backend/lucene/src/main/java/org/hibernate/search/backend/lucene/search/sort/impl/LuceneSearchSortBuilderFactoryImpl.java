/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;


public class LuceneSearchSortBuilderFactoryImpl implements LuceneSearchSortBuilderFactory {

	private final LuceneSearchContext searchContext;
	private final LuceneSearchIndexesContext indexes;

	public LuceneSearchSortBuilderFactoryImpl(LuceneSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector, SearchSort sort) {
		LuceneSearchSort luceneSort = LuceneSearchSort.from( searchContext, sort );
		luceneSort.toSortFields( collector );
	}

	@Override
	public ScoreSortBuilder score() {
		return new LuceneScoreSort.Builder( searchContext );
	}

	@Override
	public FieldSortBuilder field(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createFieldSortBuilder( searchContext );
	}

	@Override
	public DistanceSortBuilder distance(String absoluteFieldPath, GeoPoint location) {
		return indexes.field( absoluteFieldPath ).createDistanceSortBuilder( searchContext, location );
	}

	@Override
	public SearchSort indexOrder() {
		return new LuceneIndexOrderSort( searchContext );
	}

	@Override
	public CompositeSortBuilder composite() {
		return new LuceneCompositeSort.Builder( searchContext );
	}

	@Override
	public LuceneSearchSort fromLuceneSortField(SortField luceneSortField) {
		return new LuceneUserProvidedLuceneSortFieldSort( searchContext, luceneSortField );
	}

	@Override
	public LuceneSearchSort fromLuceneSort(Sort luceneSort) {
		return new LuceneUserProvidedLuceneSortSort( searchContext, luceneSort );
	}
}
