/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchDoubleFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchMetricDoubleAggregation extends AbstractElasticsearchNestableAggregation<Double> {

	private final String absoluteFieldPath;
	private final String operation;

	public ElasticsearchMetricDoubleAggregation(Builder builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.operation = builder.operation;
	}

	@Override
	protected final JsonObject doRequest(AggregationRequestContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		outerObject.add( operation, innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		return outerObject;
	}

	@Override
	protected Extractor<Double> extractor(AggregationRequestContext context) {
		return new MetricDoubleExtractor();
	}

	public static class Factory<F>
			extends
			AbstractElasticsearchCodecAwareSearchQueryElementFactory<SearchFilterableAggregationBuilder<Double>, F> {

		private final String operation;

		public Factory(ElasticsearchFieldCodec<F> codec, String operation) {
			super( codec );
			this.operation = operation;
		}

		@Override
		public SearchFilterableAggregationBuilder<Double> create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder( scope, field, operation );
		}
	}

	private static class MetricDoubleExtractor implements Extractor<Double> {
		@Override
		public Double extract(JsonObject aggregationResult, AggregationExtractContext context) {
			JsonElement value = aggregationResult.get( "value" );
			return ElasticsearchDoubleFieldCodec.INSTANCE.decode( value );
		}
	}

	private static class Builder extends AbstractBuilder<Double> implements SearchFilterableAggregationBuilder<Double> {
		private final String operation;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<?> field,
				String operation) {
			super( scope, field );
			this.operation = operation;
		}

		@Override
		public ElasticsearchMetricDoubleAggregation build() {
			return new ElasticsearchMetricDoubleAggregation( this );
		}
	}
}
