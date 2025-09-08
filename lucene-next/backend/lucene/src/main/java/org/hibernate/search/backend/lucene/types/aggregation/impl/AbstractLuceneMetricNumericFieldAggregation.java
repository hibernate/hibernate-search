/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;

/**
 * @param <F> The type of field values.
 * @param <K> The type of returned value. It can be {@code F}, {@link Double}
 * or a different type if value converters are used.
 */
public abstract class AbstractLuceneMetricNumericFieldAggregation<F, E extends Number, K>
		extends AbstractLuceneNestableAggregation<K> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	protected final AbstractLuceneNumericFieldCodec<F, E> codec;
	protected final LuceneNumericDomain<E> numericDomain;
	private final AbstractExtractorBuilder<F, E, K> extractorCreator;

	AbstractLuceneMetricNumericFieldAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.indexNames = builder.scope.hibernateSearchIndexNames();
		this.absoluteFieldPath = builder.field.absolutePath();
		this.codec = builder.codec;
		this.numericDomain = codec.getDomain();
		this.extractorCreator = builder.extractorCreator;
	}

	@Override
	public Extractor<K> request(AggregationRequestContext context) {
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromField(
				absoluteFieldPath, createNestedDocsProvider( context )
		);
		return extractorCreator.extractor( this, fillCollectors( source, context ) );
	}

	abstract List<CollectorKey<?, Long>> fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context);

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	private static class LuceneNumericMetricFieldAggregationExtraction<F, E extends Number, K> implements Extractor<K> {
		private final CollectorKey<?, Long> collectorKey;
		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

		private LuceneNumericMetricFieldAggregationExtraction(CollectorKey<?, Long> collectorKey,
				AbstractLuceneNumericFieldCodec<F, E> codec, ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
			this.collectorKey = collectorKey;
			this.codec = codec;
			this.fromFieldValueConverter = fromFieldValueConverter;
		}

		@Override
		public K extract(AggregationExtractContext context) {
			Long aggregatedValue = context.getCollectorResults( collectorKey );
			if ( aggregatedValue == null ) {
				return null;
			}
			E e = codec.getDomain().sortedDocValueToTerm( aggregatedValue );
			F decoded = codec.decode( e );
			return fromFieldValueConverter.fromDocumentValue( decoded, context.fromDocumentValueConvertContext() );
		}

		private static class Builder<F, E extends Number, K> extends AbstractExtractorBuilder<F, E, K> {
			private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

			private Builder(ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
				this.fromFieldValueConverter = fromFieldValueConverter;
			}

			@Override
			Extractor<K> extractor(AbstractLuceneMetricNumericFieldAggregation<F, E, K> aggregation,
					List<CollectorKey<?, Long>> collectorKeys) {
				return new LuceneNumericMetricFieldAggregationExtraction<>(
						collectorKeys.get( 0 ),
						aggregation.codec,
						fromFieldValueConverter
				);
			}
		}
	}

	private static class LuceneNumericMetricFieldAggregationDoubleExtractor implements Extractor<Double> {

		private final CollectorKey<?, Long> collectorKey;
		private final AbstractLuceneNumericFieldCodec<?, ?> codec;

		private LuceneNumericMetricFieldAggregationDoubleExtractor(CollectorKey<?, Long> collectorKey,
				AbstractLuceneNumericFieldCodec<?, ?> codec) {
			this.collectorKey = collectorKey;
			this.codec = codec;
		}

		@Override
		public Double extract(AggregationExtractContext context) {
			Long aggregatedValue = context.getCollectorResults( collectorKey );
			if ( aggregatedValue == null ) {
				return null;
			}

			return codec.sortedDocValueToDouble( aggregatedValue );
		}

		private static class Builder<F, E extends Number> extends AbstractExtractorBuilder<F, E, Double> {

			@Override
			Extractor<Double> extractor(AbstractLuceneMetricNumericFieldAggregation<F, E, Double> aggregation,
					List<CollectorKey<?, Long>> collectorKeys) {
				return new LuceneNumericMetricFieldAggregationDoubleExtractor(
						collectorKeys.get( 0 ),
						aggregation.codec
				);
			}
		}
	}

	private static class LuceneNumericMetricFieldAggregationRawExtraction<E extends Number, K> implements Extractor<K> {

		private final CollectorKey<?, Long> collectorKey;
		private final LuceneNumericDomain<E> numericDomain;

		private LuceneNumericMetricFieldAggregationRawExtraction(CollectorKey<?, Long> collectorKey,
				LuceneNumericDomain<E> numericDomain) {
			this.collectorKey = collectorKey;
			this.numericDomain = numericDomain;
		}

		@SuppressWarnings("unchecked")
		@Override
		public K extract(AggregationExtractContext context) {
			Long aggregatedValue = context.getCollectorResults( collectorKey );
			if ( aggregatedValue == null ) {
				return null;
			}
			return (K) numericDomain.sortedDocValueToTerm( aggregatedValue );
		}

		private static class Builder<F, E extends Number, K> extends AbstractExtractorBuilder<F, E, K> {

			@Override
			Extractor<K> extractor(AbstractLuceneMetricNumericFieldAggregation<F, E, K> aggregation,
					List<CollectorKey<?, Long>> collectorKeys) {
				return new LuceneNumericMetricFieldAggregationRawExtraction<>(
						collectorKeys.get( 0 ),
						aggregation.numericDomain
				);
			}
		}
	}

	protected abstract static class AbstractExtractorBuilder<F, E extends Number, K> {

		abstract Extractor<K> extractor(AbstractLuceneMetricNumericFieldAggregation<F, E, K> aggregation,
				List<CollectorKey<?, Long>> collectorKeys);
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

		@Override
		public <T> Builder<F, ?, T> type(Class<T> expectedType, ValueModel valueModel) {
			AbstractExtractorBuilder<F, E, T> extractorCreator;
			if ( ValueModel.RAW.equals( valueModel ) ) {
				if ( Double.class.isAssignableFrom( expectedType ) ) {
					extractorCreator = doubleExtractor();
				}
				else {
					var projectionConverter = field.type().rawProjectionConverter()
							.withConvertedType( expectedType, field );
					extractorCreator = rawExtractor( projectionConverter );
				}
			}
			else {
				var projectionConverter = field.type().projectionConverter( valueModel )
						.withConvertedType( expectedType, field );
				extractorCreator = extractor( projectionConverter );
			}

			return getFtBuilder( extractorCreator );
		}

		protected <T> AbstractExtractorBuilder<F, E, T> extractor(ProjectionConverter<F, ? extends T> projectionConverter) {
			return new LuceneNumericMetricFieldAggregationExtraction.Builder<>( projectionConverter );
		}

		// we've checked the types in the place where we are calling this method:
		protected <T> AbstractExtractorBuilder<F, E, T> rawExtractor(ProjectionConverter<?, ? extends T> projectionConverter) {
			return new LuceneNumericMetricFieldAggregationRawExtraction.Builder<>();
		}

		// we've checked the types in the place where we are calling this method:
		@SuppressWarnings("unchecked")
		protected <T> AbstractExtractorBuilder<F, E, T> doubleExtractor() {
			return (AbstractExtractorBuilder<F, E, T>) new LuceneNumericMetricFieldAggregationDoubleExtractor.Builder<>();
		}

		protected abstract <T> Builder<F, E, T> getFtBuilder(AbstractExtractorBuilder<F, E, T> extractorCreator);

	}

	protected abstract static class Builder<F, E extends Number, K> extends AbstractBuilder<K>
			implements FieldMetricAggregationBuilder<K> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final AbstractExtractorBuilder<F, E, K> extractorCreator;

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field,
				AbstractExtractorBuilder<F, E, K> extractorCreator) {
			super( scope, field );
			this.codec = codec;
			this.extractorCreator = extractorCreator;
		}
	}
}
