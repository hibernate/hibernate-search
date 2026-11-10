package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchScoreSort extends AbstractElasticsearchReversibleSort {

	private static final String SCORE_SORT_KEYWORD = "_score";
	private static final JsonNode SCORE_SORT_KEYWORD_JSON = JsonScalarConversions.toNode( SCORE_SORT_KEYWORD );

	ElasticsearchScoreSort(Builder builder) {
		super( builder );
	}

	@Override
	public void doToJsonSorts(ElasticsearchSearchSortCollector collector, ObjectNode innerObject) {
		if ( innerObject.size() == 0 ) {
			collector.collectSort( SCORE_SORT_KEYWORD_JSON );
		}
		else {
			ObjectNode outerObject = JsonNodeFactory.instance.objectNode();
			outerObject.set( SCORE_SORT_KEYWORD, innerObject );
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
