package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import tools.jackson.databind.node.ObjectNode;

class ElasticsearchUserProvidedJsonAggregation<T> extends AbstractElasticsearchAggregation<T> {

	private final ObjectNode requestJson;
	private final JsonHandler<T> handler;
	private final JsonValidation validation;

	private ElasticsearchUserProvidedJsonAggregation(Builder<T> builder) {
		super( builder );
		this.requestJson = builder.json;
		this.handler = builder.handler;
		this.validation = builder.scope.userFacingCodec().validation();
	}

	@Override
	public Extractor<T> request(AggregationRequestContext context, AggregationKey<?> key,
			ObjectNode jsonAggregations) {
		jsonAggregations.set( key.name(), requestJson );
		return new HandlerExtractor<>( key, handler, validation );
	}

	static class Builder<T> extends AbstractBuilder<T> {

		private final ObjectNode json;
		private final JsonHandler<T> handler;

		Builder(ElasticsearchSearchIndexScope<?> scope, ObjectNode json, JsonHandler<T> handler) {
			super( scope );
			this.json = json;
			this.handler = Objects.requireNonNull( handler );
		}

		@Override
		public ElasticsearchSearchAggregation<T> build() {
			return new ElasticsearchUserProvidedJsonAggregation<>( this );
		}
	}

	private record HandlerExtractor<T>(AggregationKey<?> key, JsonHandler<T> handler, JsonValidation validation)
			implements Extractor<T> {
		@Override
		public T extract(ObjectNode aggregationResult, AggregationExtractContext context) {
			return JsonHandlerBridge.exportObject( handler, aggregationResult.get( key.name() ), "aggregation result",
					validation );
		}
	}
}
