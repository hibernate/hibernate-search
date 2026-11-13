package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import tools.jackson.databind.node.ObjectNode;

class ElasticsearchSearchResultImpl<H> extends SimpleSearchResult<H>
		implements ElasticsearchSearchResult<H> {

	private final ObjectNode responseBody;
	private final String scrollId;
	private final JsonValidation validation;

	ElasticsearchSearchResultImpl(ObjectNode responseBody, SearchResultTotal resultTotal, List<H> hits,
			Map<AggregationKey<?>, ?> aggregationResults, Integer took, Boolean timedOut, String scrollId) {
		this( responseBody, resultTotal, hits, aggregationResults, took, timedOut, scrollId, JsonValidation.systemDefault() );
	}

	ElasticsearchSearchResultImpl(ObjectNode responseBody, SearchResultTotal resultTotal, List<H> hits,
			Map<AggregationKey<?>, ?> aggregationResults, Integer took, Boolean timedOut, String scrollId,
			JsonValidation validation) {
		super( resultTotal, hits, aggregationResults, ( took == null ) ? null : Duration.ofMillis( took ), timedOut );
		this.responseBody = responseBody;
		this.scrollId = scrollId;
		this.validation = validation;
	}

	@Override
	public <T> T responseBody(JsonHandler<T> handler) {
		return JsonHandlerBridge.exportObject( handler, responseBody, "search response body", validation );
	}

	public String scrollId() {
		return scrollId;
	}
}
