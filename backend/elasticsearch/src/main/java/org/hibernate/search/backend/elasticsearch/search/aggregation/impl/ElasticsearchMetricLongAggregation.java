/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchMetricLongAggregation extends AbstractElasticsearchNestableAggregation<Long> {

	private static final JsonAccessor<JsonObject> COUNT_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "value_count" ).asObject();

	private static final JsonAccessor<JsonObject> COUNT_DISTINCT_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "cardinality" ).asObject();

	private static final JsonAccessor<String> FIELD_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "field" ).asString();

	public static <F> ElasticsearchMetricLongAggregation.Factory<F> count(ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchMetricLongAggregation.Factory<>( codec, COUNT_PROPERTY_ACCESSOR );
	}

	public static <F> ElasticsearchMetricLongAggregation.Factory<F> countDistinct(ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchMetricLongAggregation.Factory<>( codec, COUNT_DISTINCT_PROPERTY_ACCESSOR );
	}

	private final String absoluteFieldPath;
	private final JsonAccessor<JsonObject> operation;

	private ElasticsearchMetricLongAggregation(Builder builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.operation = builder.operation;
	}

	@Override
	protected final JsonObject doRequest(AggregationRequestBuildingContextContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		operation.set( outerObject, innerObject );
		FIELD_PROPERTY_ACCESSOR.set( innerObject, absoluteFieldPath );
		return outerObject;
	}

	@Override
	protected Extractor<Long> extractor(AggregationRequestBuildingContextContext context) {
		return new MetricLongExtractor( nestedPathHierarchy, filter );
	}

	private static class Factory<F>
			extends
			AbstractElasticsearchCodecAwareSearchQueryElementFactory<SearchFilterableAggregationBuilder<Long>, F> {

		private final JsonAccessor<JsonObject> operation;

		private Factory(ElasticsearchFieldCodec<F> codec, JsonAccessor<JsonObject> operation) {
			super( codec );
			this.operation = operation;
		}

		@Override
		public SearchFilterableAggregationBuilder<Long> create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder( scope, field, operation );
		}
	}

	private static class MetricLongExtractor extends AbstractExtractor<Long> {
		protected MetricLongExtractor(List<String> nestedPathHierarchy, ElasticsearchSearchPredicate filter) {
			super( nestedPathHierarchy, filter );
		}

		@Override
		protected Long doExtract(JsonObject aggregationResult, AggregationExtractContext context) {
			JsonElement value = aggregationResult.get( "value" );
			return JsonElementTypes.LONG.fromElement( value );
		}
	}

	private static class Builder extends AbstractBuilder<Long> implements SearchFilterableAggregationBuilder<Long> {
		private final JsonAccessor<JsonObject> operation;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<?> field,
				JsonAccessor<JsonObject> operation) {
			super( scope, field );
			this.operation = operation;
		}

		@Override
		public ElasticsearchMetricLongAggregation build() {
			return new ElasticsearchMetricLongAggregation( this );
		}
	}
}
