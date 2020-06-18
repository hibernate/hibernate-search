/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchSearchSort implements SearchSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static SearchSort of(ElasticsearchSearchContext searchContext, ElasticsearchSearchSortBuilder builder) {
		return new ElasticsearchSearchSort( searchContext.indexes().hibernateSearchIndexNames(), builder );
	}

	public static ElasticsearchSearchSort from(ElasticsearchSearchContext searchContext, SearchSort sort) {
		if ( !( sort instanceof ElasticsearchSearchSort ) ) {
			throw log.cannotMixElasticsearchSearchSortWithOtherSorts( sort );
		}
		ElasticsearchSearchSort casted = (ElasticsearchSearchSort) sort;
		if ( !searchContext.indexes().hibernateSearchIndexNames().equals( casted.indexNames ) ) {
			throw log.sortDefinedOnDifferentIndexes( sort, casted.indexNames,
					searchContext.indexes().hibernateSearchIndexNames() );
		}
		return casted;
	}

	private final Set<String> indexNames;
	private final ElasticsearchSearchSortBuilder builder;

	private ElasticsearchSearchSort(Set<String> indexName, ElasticsearchSearchSortBuilder builder) {
		this.indexNames = indexName;
		this.builder = builder;
	}

	public void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		builder.toJsonSorts( collector );
	}
}
