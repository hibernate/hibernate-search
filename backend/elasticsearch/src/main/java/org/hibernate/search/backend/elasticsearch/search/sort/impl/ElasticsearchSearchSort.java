/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public interface ElasticsearchSearchSort extends SearchSort {

	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	Set<String> indexNames();

	void toJsonSorts(ElasticsearchSearchSortCollector collector);

	static ElasticsearchSearchSort from(ElasticsearchSearchIndexScope<?> scope, SearchSort sort) {
		if ( !( sort instanceof ElasticsearchSearchSort ) ) {
			throw log.cannotMixElasticsearchSearchSortWithOtherSorts( sort );
		}
		ElasticsearchSearchSort casted = (ElasticsearchSearchSort) sort;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw log.sortDefinedOnDifferentIndexes( sort, casted.indexNames(), scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() ) );
		}
		return casted;
	}
}
