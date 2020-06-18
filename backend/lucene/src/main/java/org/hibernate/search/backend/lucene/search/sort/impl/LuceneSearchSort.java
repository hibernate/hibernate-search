/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class LuceneSearchSort implements SearchSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// TODO HSEARCH-3476 this is just a temporary hack:
	//  we should move to one SearchSort implementation per type of sort.
	public static SearchSort of(LuceneSearchContext searchContext, LuceneSearchSortBuilder builder) {
		return new LuceneSearchSort( searchContext.indexes().indexNames(), builder );
	}

	public static LuceneSearchSort from(LuceneSearchContext searchContext, SearchSort sort) {
		if ( !( sort instanceof LuceneSearchSort ) ) {
			throw log.cannotMixLuceneSearchSortWithOtherSorts( sort );
		}
		LuceneSearchSort casted = (LuceneSearchSort) sort;
		if ( !searchContext.indexes().indexNames().equals( casted.indexNames ) ) {
			throw log.sortDefinedOnDifferentIndexes( sort, casted.indexNames, searchContext.indexes().indexNames() );
		}
		return casted;
	}

	private final Set<String> indexNames;
	private final LuceneSearchSortBuilder builder;

	LuceneSearchSort(Set<String> indexNames, LuceneSearchSortBuilder builder) {
		this.indexNames = indexNames;
		this.builder = builder;
	}

	public void toSortFields(LuceneSearchSortCollector collector) {
		builder.toSortFields( collector );
	}
}
