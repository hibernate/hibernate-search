package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * @param <F> The type of field values.
 * @param <K> The type of returned value. It can be {@code F}, {@link Double}
 * or a different type if value converters are used.
 */
public class ElasticsearchMetricFieldAggregation<F, K> extends AbstractElasticsearchNestableAggregation<K> {

	private static final JsonAccessor<ObjectNode> SUM_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "sum" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<ObjectNode> MIN_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "min" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<ObjectNode> MAX_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "max" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<ObjectNode> AVG_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "avg" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<String> FIELD_PROPERTY_ACCESSOR =
			JsonAccessor.root().property( "field" ).unwrapSingletonArrays().asString();

	private static final JsonAccessor<Double> VALUE_ACCESSOR =
			JsonAccessor.root().property( "value" ).unwrapSingletonArrays().asDouble();

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
	private final JsonAccessor<ObjectNode> operation;

	private ElasticsearchMetricFieldAggregation(Builder<F, K> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.metricFieldExtractorCreator = builder.metricFieldExtractorCreator;
		this.operation = builder.operation;
	}

	@Override
	protected final ObjectNode doRequest(AggregationRequestBuildingContextContext context) {
		ObjectNode outerObject = JsonNodeFactory.instance.objectNode();
		ObjectNode innerObject = JsonNodeFactory.instance.objectNode();

		operation.set( outerObject, innerObject );
		FIELD_PROPERTY_ACCESSOR.set( innerObject, absoluteFieldPath );
		return outerObject;
	}

	@Override
	protected Extractor<K> extractor(AggregationKey<?> key, AggregationRequestBuildingContextContext context) {
		return metricFieldExtractorCreator.extractor( key, filter );
	}

	private static class Factory<F>
			extends
			AbstractElasticsearchCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector, F> {

		private final JsonAccessor<ObjectNode> operation;

		private Factory(ElasticsearchFieldCodec<F> codec, JsonAccessor<ObjectNode> operation) {
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
		private final JsonAccessor<ObjectNode> operation;

		private TypeSelector(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field,
				JsonAccessor<ObjectNode> operation) {
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
					var projectionConverter = (ProjectionConverter<JsonNode, ? extends T>) field.type()
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

		protected MetricFieldExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter, ElasticsearchFieldCodec<F> codec) {
			super( key, nestedPathHierarchy, filter );
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.codec = codec;
		}

		@Override
		protected K doExtract(ObjectNode aggregationResult, AggregationExtractContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			Optional<Double> value = VALUE_ACCESSOR.get( aggregationResult );
			JsonNode valueAsString = aggregationResult.get( "value_as_string" );


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
			AbstractExtractor<K> extractor(AggregationKey<?> key, ElasticsearchSearchPredicate filter) {
				return new MetricFieldExtractor<>( key, nestedPathHierarchy, filter, fromFieldValueConverter, codec );
			}
		}
	}

	private static class DoubleMetricFieldExtractor extends AbstractExtractor<Double> {
		protected DoubleMetricFieldExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter) {
			super( key, nestedPathHierarchy, filter );
		}

		@Override
		protected Double doExtract(ObjectNode aggregationResult, AggregationExtractContext context) {
			Optional<Double> value = VALUE_ACCESSOR.get( aggregationResult );
			return value.orElse( null );
		}

		private static class Builder extends AggregationExtractorBuilder<Double> {

			private Builder(List<String> nestedPathHierarchy) {
				super( nestedPathHierarchy );
			}

			@Override
			AbstractExtractor<Double> extractor(AggregationKey<?> key, ElasticsearchSearchPredicate filter) {
				return new DoubleMetricFieldExtractor( key, nestedPathHierarchy, filter );
			}
		}
	}

	private static class RawMetricFieldExtractor<K> extends AbstractExtractor<K> {

		private final ProjectionConverter<JsonNode, K> projectionConverter;

		protected RawMetricFieldExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter,
				ProjectionConverter<JsonNode, K> projectionConverter) {
			super( key, nestedPathHierarchy, filter );
			this.projectionConverter = projectionConverter;
		}

		@Override
		protected K doExtract(ObjectNode aggregationResult, AggregationExtractContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			return projectionConverter.fromDocumentValue( aggregationResult, convertContext );
		}

		private static class Builder<K> extends AggregationExtractorBuilder<K> {
			private final ProjectionConverter<JsonNode, K> projectionConverter;

			private Builder(List<String> nestedPathHierarchy, ProjectionConverter<JsonNode, K> projectionConverter) {
				super( nestedPathHierarchy );
				this.projectionConverter = projectionConverter;
			}

			@Override
			AbstractExtractor<K> extractor(AggregationKey<?> key, ElasticsearchSearchPredicate filter) {
				return new RawMetricFieldExtractor<>( key, nestedPathHierarchy, filter, projectionConverter );
			}
		}
	}

	private abstract static class AggregationExtractorBuilder<K> {
		protected final List<String> nestedPathHierarchy;

		protected AggregationExtractorBuilder(List<String> nestedPathHierarchy) {
			this.nestedPathHierarchy = nestedPathHierarchy;
		}

		abstract AbstractExtractor<K> extractor(AggregationKey<?> key, ElasticsearchSearchPredicate filter);
	}

	private static class Builder<F, K> extends AbstractBuilder<K>
			implements FieldMetricAggregationBuilder<K> {

		private final AggregationExtractorBuilder<K> metricFieldExtractorCreator;
		private final JsonAccessor<ObjectNode> operation;

		private Builder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				AggregationExtractorBuilder<K> metricFieldExtractorCreator,
				JsonAccessor<ObjectNode> operation) {
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
