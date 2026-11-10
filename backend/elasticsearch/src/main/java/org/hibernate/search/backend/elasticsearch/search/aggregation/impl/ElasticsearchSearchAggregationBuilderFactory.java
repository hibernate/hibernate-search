package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.spi.WithParametersAggregationBuilder;
import org.hibernate.search.util.json.JsonHandler;

public class ElasticsearchSearchAggregationBuilderFactory
		implements SearchAggregationBuilderFactory {

	private final ElasticsearchSearchIndexScope<?> scope;

	public ElasticsearchSearchAggregationBuilderFactory(ElasticsearchSearchIndexScope<?> scope) {
		this.scope = scope;
	}

	public <T> SearchAggregationBuilder<T> fromJson(JsonHandler<T> handler, T jsonObject) {
		return new ElasticsearchUserProvidedJsonAggregation.Builder<>( scope,
				JsonHandlerBridge.importObject( handler, jsonObject, "aggregation import" ), handler );
	}

	public <T> SearchAggregationBuilder<T> fromJson(String jsonString, JsonHandler<T> resultHandler) {
		return new ElasticsearchUserProvidedJsonAggregation.Builder<>( scope,
				JsonHandlerBridge.parseObject( jsonString, "aggregation import" ), resultHandler );
	}

	@Override
	public <T> WithParametersAggregationBuilder<T> withParameters() {
		return new ElasticsearchWithParametersAggregation.Builder<>( scope );
	}

	@Override
	public <T> CompositeAggregationBuilder<T> compositeAggregation() {
		return new ElasticsearchCompositeAggregation.Builder<>( scope );
	}
}
