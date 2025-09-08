/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CompensatedSum;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.DoubleAggregationFunctionCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;

/**
 * @param <F> The type of field values.
 * @param <K> The type of returned value. It can be {@code F}, {@link Double}
 * or a different type if value converters are used.
 */
public abstract class AbstractLuceneMetricCompensatedSumAggregation<F, E extends Number, K>
		extends AbstractLuceneNestableAggregation<K> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	protected final AbstractLuceneNumericFieldCodec<F, E> codec;
	private final LuceneNumericDomain<E> numericDomain;
	private final ExtractedValueConverter<E, ? extends K> extractedConverter;

	protected CollectorKey<?, Long> collectorKey;
	protected CollectorKey<DoubleAggregationFunctionCollector<CompensatedSum>, Double> compensatedSumCollectorKey;

	AbstractLuceneMetricCompensatedSumAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.indexNames = builder.scope.hibernateSearchIndexNames();
		this.absoluteFieldPath = builder.field.absolutePath();
		this.codec = builder.codec;
		this.numericDomain = codec.getDomain();
		this.extractedConverter = builder.extractedConverter;
	}

	@Override
	public Extractor<K> request(AggregationRequestContext context) {
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromField(
				absoluteFieldPath, createNestedDocsProvider( context )
		);
		fillCollectors( source, context );
		return new LuceneNumericMetricFieldAggregationExtraction();
	}

	abstract void fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context);

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	private class LuceneNumericMetricFieldAggregationExtraction implements Extractor<K> {

		@Override
		public K extract(AggregationExtractContext context) {
			E extracted = extractEncoded( context, numericDomain );

			return extractedConverter.convert( extracted, context.fromDocumentValueConvertContext() );
		}
	}

	abstract E extractEncoded(AggregationExtractContext context, LuceneNumericDomain<E> numericDomain);

	protected abstract static class ExtractedValueConverter<E extends Number, K> {

		abstract K convert(E extracted, FromDocumentValueConvertContext context);
	}

	protected abstract static class TypeSelector<F, E extends Number> implements FieldMetricAggregationBuilder.TypeSelector {
		protected final AbstractLuceneNumericFieldCodec<F, E> codec;
		protected final LuceneSearchIndexScope<?> scope;
		protected final LuceneSearchIndexValueFieldContext<F> field;

		protected TypeSelector(AbstractLuceneNumericFieldCodec<F, E> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Builder<F, ?, T> type(Class<T> expectedType, ValueModel valueModel) {
			ExtractedValueConverter<E, ? extends T> extractedConverter;

			if ( ValueModel.RAW.equals( valueModel ) ) {
				if ( Double.class.isAssignableFrom( expectedType ) ) {
					extractedConverter = (ExtractedValueConverter<E, ? extends T>) new DoubleExtractedValueConverter<>();
				}
				else {
					var projectionConverter = (ProjectionConverter<E, T>) field.type().rawProjectionConverter()
							.withConvertedType( expectedType, field );
					extractedConverter = new RawExtractedValueConverter<>( projectionConverter );
				}
			}
			else {
				var projectionConverter = field.type().projectionConverter( valueModel )
						.withConvertedType( expectedType, field );
				extractedConverter = new DecodingExtractedValueConverter<>( projectionConverter, codec );
			}

			return getFtBuilder( extractedConverter );
		}

		protected abstract <T> Builder<F, ? extends Number, T> getFtBuilder(
				ExtractedValueConverter<E, ? extends T> extractedConverter);
	}

	private static class DoubleExtractedValueConverter<E extends Number> extends ExtractedValueConverter<E, Double> {

		@Override
		Double convert(E extracted, FromDocumentValueConvertContext context) {
			return extracted == null ? null : extracted.doubleValue();
		}
	}

	private static class RawExtractedValueConverter<E extends Number, T> extends ExtractedValueConverter<E, T> {
		private final ProjectionConverter<E, T> projectionConverter;

		private RawExtractedValueConverter(ProjectionConverter<E, T> projectionConverter) {
			this.projectionConverter = projectionConverter;
		}

		@Override
		T convert(E extracted, FromDocumentValueConvertContext context) {
			return extracted == null ? null : projectionConverter.fromDocumentValue( extracted, context );
		}
	}

	private static class DecodingExtractedValueConverter<E extends Number, F, T> extends ExtractedValueConverter<E, T> {
		private final ProjectionConverter<F, T> projectionConverter;
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private DecodingExtractedValueConverter(ProjectionConverter<F, T> projectionConverter,
				AbstractLuceneNumericFieldCodec<F, E> codec) {
			this.projectionConverter = projectionConverter;
			this.codec = codec;
		}

		@Override
		T convert(E extracted, FromDocumentValueConvertContext context) {
			return extracted == null ? null : projectionConverter.fromDocumentValue( codec.decode( extracted ), context );
		}
	}

	protected abstract static class Builder<F, E extends Number, K> extends AbstractBuilder<K>
			implements FieldMetricAggregationBuilder<K> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final ExtractedValueConverter<E, ? extends K> extractedConverter;

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field,
				ExtractedValueConverter<E, ? extends K> extractedConverter) {
			super( scope, field );
			this.codec = codec;
			this.extractedConverter = extractedConverter;
		}
	}
}
