package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import tools.jackson.databind.node.ObjectNode;

class ElasticsearchUserProvidedJsonSort extends AbstractElasticsearchSort {

	private final ObjectNode json;

	ElasticsearchUserProvidedJsonSort(ElasticsearchSearchIndexScope<?> scope, ObjectNode json) {
		super( scope );
		this.json = json;
	}

	@Override
	public void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		collector.collectSort( json );
	}

}
