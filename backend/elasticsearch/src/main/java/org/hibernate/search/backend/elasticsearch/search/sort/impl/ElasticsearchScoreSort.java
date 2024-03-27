/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class ElasticsearchScoreSort extends AbstractElasticsearchReversibleSort {

	private static final String SCORE_SORT_KEYWORD = "_score";
	private static final JsonPrimitive SCORE_SORT_KEYWORD_JSON = new JsonPrimitive( SCORE_SORT_KEYWORD );

	ElasticsearchScoreSort(Builder builder) {
		super( builder );
	}

	@Override
	public void doToJsonSorts(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( innerObject.size() == 0 ) {
			collector.collectSort( SCORE_SORT_KEYWORD_JSON );
		}
		else {
			JsonObject outerObject = new JsonObject();
			outerObject.add( SCORE_SORT_KEYWORD, innerObject );
			collector.collectSort( outerObject );
		}
	}

	static class Builder extends AbstractBuilder implements ScoreSortBuilder {

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public SearchSort build() {
			return new ElasticsearchScoreSort( this );
		}
	}
}
