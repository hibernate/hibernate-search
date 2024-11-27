/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;

import org.apache.lucene.search.Sort;

class LuceneUserProvidedLuceneSortSort extends AbstractLuceneSort {

	private final Sort luceneSort;

	LuceneUserProvidedLuceneSortSort(LuceneSearchIndexScope<?> scope, Sort luceneSort) {
		super( scope );
		this.luceneSort = luceneSort;
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		collector.collectSortFields( luceneSort.getSort() );
	}
}
