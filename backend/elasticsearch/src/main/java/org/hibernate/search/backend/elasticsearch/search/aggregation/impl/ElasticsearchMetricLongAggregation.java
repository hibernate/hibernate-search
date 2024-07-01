/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLongFieldCodec;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchMetricLongAggregation extends AbstractElasticsearchNestableAggregation<Long> {

	private final String absoluteFieldPath;
	private final String operation;

	public ElasticsearchMetricLongAggregation(Builder builder) {
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
	protected Extractor<Long> extractor(AggregationRequestContext context) {
		return new MetricLongExtractor( nestedPathHierarchy, filter );
	}

	public static class Factory<F>
			extends
			AbstractElasticsearchCodecAwareSearchQueryElementFactory<SearchFilterableAggregationBuilder<Long>, F> {

		private final String operation;

		public Factory(ElasticsearchFieldCodec<F> codec, String operation) {
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
			return ElasticsearchLongFieldCodec.INSTANCE.decode( value );
		}
	}

	private static class Builder extends AbstractBuilder<Long> implements SearchFilterableAggregationBuilder<Long> {
		private final String operation;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<?> field,
				String operation) {
			super( scope, field );
			this.operation = operation;
		}

		@Override
		public ElasticsearchMetricLongAggregation build() {
			return new ElasticsearchMetricLongAggregation( this );
		}
	}
}
