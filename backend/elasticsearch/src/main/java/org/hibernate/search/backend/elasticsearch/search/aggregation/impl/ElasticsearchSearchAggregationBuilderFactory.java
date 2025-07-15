/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.spi.WithParametersAggregationBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchSearchAggregationBuilderFactory
		implements SearchAggregationBuilderFactory {

	private final ElasticsearchSearchIndexScope<?> scope;

	public ElasticsearchSearchAggregationBuilderFactory(ElasticsearchSearchIndexScope<?> scope) {
		this.scope = scope;
	}

	public SearchAggregationBuilder<JsonObject> fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonAggregation.Builder( scope, jsonObject );
	}

	public SearchAggregationBuilder<JsonObject> fromJson(String jsonString) {
		return fromJson( scope.userFacingGson().fromJson( jsonString, JsonObject.class ) );
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
