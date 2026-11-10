package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import tools.jackson.databind.JsonNode;

class ElasticsearchIndexOrderSort extends AbstractElasticsearchSort {

	private static final JsonNode DOC_SORT_KEYWORD_JSON = JsonScalarConversions.toNode( "_doc" );

	ElasticsearchIndexOrderSort(ElasticsearchSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		collector.collectSort( DOC_SORT_KEYWORD_JSON );
	}
}
