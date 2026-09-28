package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;

import org.apache.lucene.search.SortField;

class LuceneIndexOrderSort extends AbstractLuceneSort {

	LuceneIndexOrderSort(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		collector.collectSortField( SortField.FIELD_DOC );
	}
}
