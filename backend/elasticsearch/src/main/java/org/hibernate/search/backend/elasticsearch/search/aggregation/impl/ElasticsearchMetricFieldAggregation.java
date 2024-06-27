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
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <F> The type of field values.
 * @param <K> The type of returned value. It can be {@code F}
 * or a different type if value converters are used.
 */
public class ElasticsearchMetricFieldAggregation<F, K> extends AbstractElasticsearchNestableAggregation<K> {

	private final String absoluteFieldPath;
	private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
	private final ElasticsearchFieldCodec<F> codec;
	private final String operation;

	public ElasticsearchMetricFieldAggregation(Builder<F, K> builder) {
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

		outerObject.add( operation, innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		return outerObject;
	}

	@Override
	protected Extractor<K> extractor(AggregationRequestContext context) {
		return new MetricFieldExtractor();
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector, F> {

		private final String operation;

		public Factory(ElasticsearchFieldCodec<F> codec, String operation) {
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
		private final String operation;

		private TypeSelector(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field,
				String operation) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
			this.operation = operation;
		}

		@Override
		public <T> Builder<F, T> type(Class<T> expectedType, ValueConvert convert) {
			ProjectionConverter<F, ? extends T> projectionConverter = null;
			if ( !Double.class.isAssignableFrom( expectedType )
					||
					field.type().projectionConverter( convert ).valueType().isAssignableFrom( expectedType ) ) {
				projectionConverter = field.type().projectionConverter( convert )
						.withConvertedType( expectedType, field );
			}
			return new Builder<>( codec, scope, field,
					projectionConverter,
					operation
			);
		}
	}

	@SuppressWarnings("unchecked")
	private class MetricFieldExtractor implements Extractor<K> {
		@Override
		public K extract(JsonObject aggregationResult, AggregationExtractContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			JsonElement value = aggregationResult.get( "value" );
			JsonElement valueAsString = aggregationResult.get( "value_as_string" );

			if ( fromFieldValueConverter == null ) {
				Double decode = ElasticsearchDoubleFieldCodec.INSTANCE.decode( value );
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
		private final String operation;

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter, String operation) {
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
