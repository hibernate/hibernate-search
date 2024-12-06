/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;

public interface LuceneSearchSort extends SearchSort {

	Set<String> indexNames();

	void toSortFields(LuceneSearchSortCollector collector);

	static LuceneSearchSort from(LuceneSearchIndexScope<?> scope, SearchSort sort) {
		if ( !( sort instanceof LuceneSearchSort ) ) {
			throw QueryLog.INSTANCE.cannotMixLuceneSearchSortWithOtherSorts( sort );
		}
		LuceneSearchSort casted = (LuceneSearchSort) sort;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw QueryLog.INSTANCE.sortDefinedOnDifferentIndexes( sort, casted.indexNames(), scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() ) );
		}
		return casted;
	}
}
