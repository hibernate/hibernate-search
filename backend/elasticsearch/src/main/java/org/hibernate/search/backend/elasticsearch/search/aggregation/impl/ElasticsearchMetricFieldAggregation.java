/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <F> The type of field values.
 * @param <K> The type of returned value. It can be {@code F}, {@link Double}
 * or a different type if value converters are used.
 */
public class ElasticsearchMetricFieldAggregation<F, K> extends AbstractElasticsearchNestableAggregation<K> {

	private static final JsonAccessor<JsonObject> SUM_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "sum" ).asObject();

	private static final JsonAccessor<JsonObject> MIN_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "min" ).asObject();

	private static final JsonAccessor<JsonObject> MAX_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "max" ).asObject();

	private static final JsonAccessor<JsonObject> AVG_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "avg" ).asObject();

	private static final JsonAccessor<String> FIELD_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "field" ).asString();

	private static final JsonAccessor<Double> VALUE_ACCESSOR =
			JsonAccessor.root().property( "value" ).asDouble();

	public static <F> ElasticsearchMetricFieldAggregation.Factory<F> sum(ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchMetricFieldAggregation.Factory<>( codec, SUM_PROPERTY_ACCESSOR );
	}

	public static <F> ElasticsearchMetricFieldAggregation.Factory<F> min(ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchMetricFieldAggregation.Factory<>( codec, MIN_PROPERTY_ACCESSOR );
	}

	public static <F> ElasticsearchMetricFieldAggregation.Factory<F> max(ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchMetricFieldAggregation.Factory<>( codec, MAX_PROPERTY_ACCESSOR );
	}

	public static <F> ElasticsearchMetricFieldAggregation.Factory<F> avg(ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchMetricFieldAggregation.Factory<>( codec, AVG_PROPERTY_ACCESSOR );
	}

	private final String absoluteFieldPath;
	private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
	private final ElasticsearchFieldCodec<F> codec;
	private final JsonAccessor<JsonObject> operation;

	private ElasticsearchMetricFieldAggregation(Builder<F, K> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.codec = builder.codec;
		this.operation = builder.operation;
	}

	@Override
	protected final JsonObject doRequest(AggregationRequestContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		operation.set( outerObject, innerObject );
		FIELD_PROPERTY_ACCESSOR.set( innerObject, absoluteFieldPath );
		return outerObject;
	}

	@Override
	protected Extractor<K> extractor(AggregationRequestContext context) {
		return new MetricFieldExtractor( nestedPathHierarchy, filter );
	}

	private static class Factory<F>
			extends
			AbstractElasticsearchCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector, F> {

		private final JsonAccessor<JsonObject> operation;

		private Factory(ElasticsearchFieldCodec<F> codec, JsonAccessor<JsonObject> operation) {
			super( codec );
			this.operation = operation;
		}

		@Override
		public FieldMetricAggregationBuilder.TypeSelector create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new ElasticsearchMetricFieldAggregation.TypeSelector<>( codec, scope, field, operation );
		}
	}

	private static class TypeSelector<F> implements FieldMetricAggregationBuilder.TypeSelector {
		private final ElasticsearchFieldCodec<F> codec;
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<F> field;
		private final JsonAccessor<JsonObject> operation;

		private TypeSelector(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field,
				JsonAccessor<JsonObject> operation) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
			this.operation = operation;
		}

		@Override
		public <T> Builder<F, T> type(Class<T> expectedType, ValueModel valueModel) {
			ProjectionConverter<F, ? extends T> projectionConverter = null;
			if ( useProjectionConverter( expectedType, valueModel ) ) {
				projectionConverter = field.type().projectionConverter( valueModel )
						.withConvertedType( expectedType, field );
			}
			return new Builder<>( codec, scope, field,
					projectionConverter,
					operation
			);
		}

		private <T> boolean useProjectionConverter(Class<T> expectedType, ValueModel valueModel) {
			if ( !Double.class.isAssignableFrom( expectedType ) ) {
				if ( ValueModel.RAW.equals( valueModel ) ) {
					throw new AssertionFailure(
							"Raw projection converter is not supported with metric aggregations at the moment" );
				}
				return true;
			}

			// expectedType == Double.class
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return false;
			}
			return field.type().projectionConverter( valueModel ).valueType().isAssignableFrom( Double.class );
		}
	}

	private class MetricFieldExtractor extends AbstractExtractor<K> {
		protected MetricFieldExtractor(List<String> nestedPathHierarchy, ElasticsearchSearchPredicate filter) {
			super( nestedPathHierarchy, filter );
		}

		@Override
		@SuppressWarnings("unchecked")
		protected K doExtract(JsonObject aggregationResult, AggregationExtractContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			Optional<Double> value = VALUE_ACCESSOR.get( aggregationResult );
			JsonElement valueAsString = aggregationResult.get( "value_as_string" );

			if ( fromFieldValueConverter == null ) {
				Double decode = value.orElse( null );
				return (K) decode;
			}
			return fromFieldValueConverter.fromDocumentValue(
					codec.decodeAggregationValue( value, valueAsString ),
					convertContext
			);
		}
	}

	private static class Builder<F, K> extends AbstractBuilder<K>
			implements FieldMetricAggregationBuilder<K> {

		private final ElasticsearchFieldCodec<F> codec;
		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
		private final JsonAccessor<JsonObject> operation;

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter, JsonAccessor<JsonObject> operation) {
			super( scope, field );
			this.codec = codec;
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.operation = operation;
		}

		@Override
		public ElasticsearchMetricFieldAggregation<F, K> build() {
			return new ElasticsearchMetricFieldAggregation<>( this );
		}
	}
}
