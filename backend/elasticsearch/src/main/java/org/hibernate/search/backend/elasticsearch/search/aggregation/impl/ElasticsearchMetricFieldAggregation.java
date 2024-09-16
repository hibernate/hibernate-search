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
	private final AggregationExtractorBuilder<K> metricFieldExtractorCreator;
	private final JsonAccessor<JsonObject> operation;

	private ElasticsearchMetricFieldAggregation(Builder<F, K> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.metricFieldExtractorCreator = builder.metricFieldExtractorCreator;
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
		return metricFieldExtractorCreator.extractor( filter );
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

		@SuppressWarnings("unchecked")
		@Override
		public <T> Builder<F, T> type(Class<T> expectedType, ValueModel valueModel) {
			AggregationExtractorBuilder<T> metricFieldExtractorCreator;

			if ( ValueModel.RAW.equals( valueModel ) ) {
				if ( Double.class.isAssignableFrom( expectedType ) ) {
					metricFieldExtractorCreator = (AggregationExtractorBuilder<
							T>) new DoubleMetricFieldExtractor.Builder( field.nestedPathHierarchy() );
				}
				else {
					var projectionConverter = (ProjectionConverter<JsonElement, ? extends T>) field.type()
							.rawProjectionConverter().withConvertedType( expectedType, field );
					metricFieldExtractorCreator = (AggregationExtractorBuilder<T>) new RawMetricFieldExtractor.Builder<>(
							field.nestedPathHierarchy(),
							projectionConverter );
				}
			}
			else {
				var projectionConverter = field.type()
						.projectionConverter( valueModel ).withConvertedType( expectedType, field );
				metricFieldExtractorCreator =
						new MetricFieldExtractor.Builder<>( field.nestedPathHierarchy(), projectionConverter, codec );
			}

			return new Builder<>( scope, field, metricFieldExtractorCreator, operation );
		}
	}

	private static class MetricFieldExtractor<F, K> extends AbstractExtractor<K> {

		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
		private final ElasticsearchFieldCodec<F> codec;

		protected MetricFieldExtractor(List<String> nestedPathHierarchy, ElasticsearchSearchPredicate filter,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter, ElasticsearchFieldCodec<F> codec) {
			super( nestedPathHierarchy, filter );
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.codec = codec;
		}

		@Override
		protected K doExtract(JsonObject aggregationResult, AggregationExtractContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			Optional<Double> value = VALUE_ACCESSOR.get( aggregationResult );
			JsonElement valueAsString = aggregationResult.get( "value_as_string" );


			return fromFieldValueConverter.fromDocumentValue(
					codec.decodeAggregationValue( value, valueAsString ),
					convertContext
			);
		}

		private static class Builder<F, K> extends AggregationExtractorBuilder<K> {
			private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
			private final ElasticsearchFieldCodec<F> codec;

			private Builder(List<String> nestedPathHierarchy, ProjectionConverter<F, ? extends K> fromFieldValueConverter,
					ElasticsearchFieldCodec<F> codec) {
				super( nestedPathHierarchy );
				this.fromFieldValueConverter = fromFieldValueConverter;
				this.codec = codec;
			}

			@Override
			AbstractExtractor<K> extractor(ElasticsearchSearchPredicate filter) {
				return new MetricFieldExtractor<>( nestedPathHierarchy, filter, fromFieldValueConverter, codec );
			}
		}
	}

	private static class DoubleMetricFieldExtractor extends AbstractExtractor<Double> {
		protected DoubleMetricFieldExtractor(List<String> nestedPathHierarchy, ElasticsearchSearchPredicate filter) {
			super( nestedPathHierarchy, filter );
		}

		@Override
		protected Double doExtract(JsonObject aggregationResult, AggregationExtractContext context) {
			Optional<Double> value = VALUE_ACCESSOR.get( aggregationResult );
			return value.orElse( null );
		}

		private static class Builder extends AggregationExtractorBuilder<Double> {

			private Builder(List<String> nestedPathHierarchy) {
				super( nestedPathHierarchy );
			}

			@Override
			AbstractExtractor<Double> extractor(ElasticsearchSearchPredicate filter) {
				return new DoubleMetricFieldExtractor( nestedPathHierarchy, filter );
			}
		}
	}

	private static class RawMetricFieldExtractor<K> extends AbstractExtractor<K> {

		private final ProjectionConverter<JsonElement, K> projectionConverter;

		protected RawMetricFieldExtractor(List<String> nestedPathHierarchy, ElasticsearchSearchPredicate filter,
				ProjectionConverter<JsonElement, K> projectionConverter) {
			super( nestedPathHierarchy, filter );
			this.projectionConverter = projectionConverter;
		}

		@Override
		protected K doExtract(JsonObject aggregationResult, AggregationExtractContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			return projectionConverter.fromDocumentValue( aggregationResult, convertContext );
		}

		private static class Builder<K> extends AggregationExtractorBuilder<K> {
			private final ProjectionConverter<JsonElement, K> projectionConverter;

			private Builder(List<String> nestedPathHierarchy, ProjectionConverter<JsonElement, K> projectionConverter) {
				super( nestedPathHierarchy );
				this.projectionConverter = projectionConverter;
			}

			@Override
			AbstractExtractor<K> extractor(ElasticsearchSearchPredicate filter) {
				return new RawMetricFieldExtractor<>( nestedPathHierarchy, filter, projectionConverter );
			}
		}
	}

	private abstract static class AggregationExtractorBuilder<K> {
		protected final List<String> nestedPathHierarchy;

		protected AggregationExtractorBuilder(List<String> nestedPathHierarchy) {
			this.nestedPathHierarchy = nestedPathHierarchy;
		}

		abstract AbstractExtractor<K> extractor(ElasticsearchSearchPredicate filter);
	}

	private static class Builder<F, K> extends AbstractBuilder<K>
			implements FieldMetricAggregationBuilder<K> {

		private final AggregationExtractorBuilder<K> metricFieldExtractorCreator;
		private final JsonAccessor<JsonObject> operation;

		private Builder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				AggregationExtractorBuilder<K> metricFieldExtractorCreator,
				JsonAccessor<JsonObject> operation) {
			super( scope, field );
			this.metricFieldExtractorCreator = metricFieldExtractorCreator;
			this.operation = operation;
		}

		@Override
		public ElasticsearchMetricFieldAggregation<F, K> build() {
			return new ElasticsearchMetricFieldAggregation<>( this );
		}
	}
}
