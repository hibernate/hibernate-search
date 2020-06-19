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

public interface LuceneSearchSort extends SearchSort {
	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	Set<String> indexNames();

	void toSortFields(LuceneSearchSortCollector collector);

	static LuceneSearchSort from(LuceneSearchContext searchContext, SearchSort sort) {
		if ( !( sort instanceof LuceneSearchSort ) ) {
			throw log.cannotMixLuceneSearchSortWithOtherSorts( sort );
		}
		LuceneSearchSort casted = (LuceneSearchSort) sort;
		if ( !searchContext.indexes().indexNames().equals( casted.indexNames() ) ) {
			throw log.sortDefinedOnDifferentIndexes( sort, casted.indexNames(), searchContext.indexes().indexNames() );
		}
		return casted;
	}
}
