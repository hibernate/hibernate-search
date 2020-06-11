/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;


public class LuceneSearchSortBuilderFactoryImpl implements LuceneSearchSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	private final LuceneSearchIndexesContext indexes;

	public LuceneSearchSortBuilderFactoryImpl(LuceneSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public SearchSort toSearchSort(List<LuceneSearchSortBuilder> builders) {
		return new LuceneSearchSort( indexes.indexNames(), builders );
	}

	@Override
	public LuceneSearchSortBuilder toImplementation(SearchSort sort) {
		if ( !( sort instanceof LuceneSearchSort ) ) {
			throw log.cannotMixLuceneSearchSortWithOtherSorts( sort );
		}
		LuceneSearchSort casted = (LuceneSearchSort) sort;
		if ( !indexes.indexNames().equals( casted.getIndexNames() ) ) {
			throw log.sortDefinedOnDifferentIndexes( sort, casted.getIndexNames(), indexes.indexNames() );
		}
		return casted;
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector, LuceneSearchSortBuilder builder) {
		builder.buildAndContribute( collector );
	}

	@Override
	public ScoreSortBuilder<LuceneSearchSortBuilder> score() {
		return new LuceneScoreSortBuilder();
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> field(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createFieldSortBuilder( searchContext );
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> distance(String absoluteFieldPath, GeoPoint location) {
		return indexes.field( absoluteFieldPath ).createDistanceSortBuilder( location );
	}

	@Override
	public LuceneSearchSortBuilder indexOrder() {
		return LuceneIndexOrderSortBuilder.INSTANCE;
	}

	@Override
	public LuceneSearchSortBuilder fromLuceneSortField(SortField luceneSortField) {
		return new LuceneUserProvidedLuceneSortFieldSortBuilder( luceneSortField );
	}

	@Override
	public LuceneSearchSortBuilder fromLuceneSort(Sort luceneSort) {
		return new LuceneUserProvidedLuceneSortSortBuilder( luceneSort );
	}
}
