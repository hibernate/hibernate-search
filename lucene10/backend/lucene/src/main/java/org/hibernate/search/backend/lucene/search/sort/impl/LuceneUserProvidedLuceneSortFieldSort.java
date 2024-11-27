/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;

import org.apache.lucene.search.SortField;

class LuceneUserProvidedLuceneSortFieldSort extends AbstractLuceneSort {

	private final SortField luceneSortField;

	LuceneUserProvidedLuceneSortFieldSort(LuceneSearchIndexScope<?> scope, SortField luceneSortField) {
		super( scope );
		this.luceneSortField = luceneSortField;
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		collector.collectSortField( luceneSortField );
	}
}
