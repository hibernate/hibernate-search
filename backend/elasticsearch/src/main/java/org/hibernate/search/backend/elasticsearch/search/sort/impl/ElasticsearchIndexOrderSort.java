/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import com.google.gson.JsonPrimitive;

class ElasticsearchIndexOrderSort extends AbstractElasticsearchSort {

	private static final JsonPrimitive DOC_SORT_KEYWORD_JSON = new JsonPrimitive( "_doc" );

	ElasticsearchIndexOrderSort(ElasticsearchSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		collector.collectSort( DOC_SORT_KEYWORD_JSON );
	}
}
