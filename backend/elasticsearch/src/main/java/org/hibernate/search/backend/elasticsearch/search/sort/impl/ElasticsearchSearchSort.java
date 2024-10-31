/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;

public interface ElasticsearchSearchSort extends SearchSort {

	Set<String> indexNames();

	void toJsonSorts(ElasticsearchSearchSortCollector collector);

	static ElasticsearchSearchSort from(ElasticsearchSearchIndexScope<?> scope, SearchSort sort) {
		if ( !( sort instanceof ElasticsearchSearchSort ) ) {
			throw QueryLog.INSTANCE.cannotMixElasticsearchSearchSortWithOtherSorts( sort );
		}
		ElasticsearchSearchSort casted = (ElasticsearchSearchSort) sort;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw QueryLog.INSTANCE.sortDefinedOnDifferentIndexes( sort, casted.indexNames(), scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() ) );
		}
		return casted;
	}
}
