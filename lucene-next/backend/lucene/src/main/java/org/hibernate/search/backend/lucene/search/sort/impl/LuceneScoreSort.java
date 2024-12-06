/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;

class LuceneScoreSort extends AbstractLuceneReversibleSort {

	private static final SortField FIELD_SCORE_ASC = new SortField( null, Type.SCORE, true );

	LuceneScoreSort(Builder builder) {
		super( builder );
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		if ( order == SortOrder.ASC ) {
			collector.collectSortField( FIELD_SCORE_ASC );
		}
		else {
			collector.collectSortField( SortField.FIELD_SCORE );
		}
	}

	static class Builder extends AbstractBuilder implements ScoreSortBuilder {
		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public SearchSort build() {
			return new LuceneScoreSort( this );
		}
	}
}
