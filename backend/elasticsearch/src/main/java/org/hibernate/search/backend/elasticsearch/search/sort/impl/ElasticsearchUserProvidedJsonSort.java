/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import com.google.gson.JsonObject;

class ElasticsearchUserProvidedJsonSort extends AbstractElasticsearchSort {

	private final JsonObject json;

	ElasticsearchUserProvidedJsonSort(ElasticsearchSearchIndexScope<?> scope, JsonObject json) {
		super( scope );
		this.json = json;
	}

	@Override
	public void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		collector.collectSort( json );
	}

}
