/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CompensatedSum;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CompensatedSumCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.DoubleAggregationFunctionCollector;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.MaxCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.MinCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
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
public class LuceneMetricCompensatedSumAggregation<F, E extends Number, K> extends AbstractLuceneNestableAggregation<K> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final AbstractLuceneNumericFieldCodec<F, E> codec;
	private final LuceneNumericDomain<E> numericDomain;
	private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
	private final String operation;

	private CollectorKey<?, Long> collectorKey;
	private CollectorKey<DoubleAggregationFunctionCollector<CompensatedSum>, Double> compensatedSumCollectorKey;

	LuceneMetricCompensatedSumAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.indexNames = builder.scope.hibernateSearchIndexNames();
		this.absoluteFieldPath = builder.field.absolutePath();
		this.codec = builder.codec;
		this.numericDomain = codec.getDomain();
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.operation = builder.operation;
	}

	@Override
	public Extractor<K> request(AggregationRequestContext context) {
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromField(
				absoluteFieldPath, createNestedDocsProvider( context )
		);
		if ( "sum".equals( operation ) ) {
			CompensatedSumCollectorFactory collectorFactory = new CompensatedSumCollectorFactory( source,
					numericDomain::sortedDocValueToDouble );
			compensatedSumCollectorKey = collectorFactory.getCollectorKey();
			context.requireCollector( collectorFactory );
		}
		else if ( "min".equals( operation ) ) {
			MinCollectorFactory collectorFactory = new MinCollectorFactory( source );
			collectorKey = collectorFactory.getCollectorKey();
			context.requireCollector( collectorFactory );
		}
		else if ( "max".equals( operation ) ) {
			MaxCollectorFactory collectorFactory = new MaxCollectorFactory( source );
			collectorKey = collectorFactory.getCollectorKey();
			context.requireCollector( collectorFactory );
		}
		else if ( "avg".equals( operation ) ) {
			CompensatedSumCollectorFactory sumCollectorFactory = new CompensatedSumCollectorFactory( source,
					numericDomain::sortedDocValueToDouble );
			compensatedSumCollectorKey = sumCollectorFactory.getCollectorKey();
			context.requireCollector( sumCollectorFactory );

			CountCollectorFactory countCollectorFactory = new CountCollectorFactory( source );
			collectorKey = countCollectorFactory.getCollectorKey();
			context.requireCollector( countCollectorFactory );
		}
		return new LuceneNumericMetricFieldAggregationExtraction();
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	private class LuceneNumericMetricFieldAggregationExtraction implements Extractor<K> {

		@Override
		public K extract(AggregationExtractContext context) {
			E extracted;

			if ( "sum".equals( operation ) ) {
				Double sum = context.getFacets( compensatedSumCollectorKey );
				extracted = numericDomain.doubleToTerm( sum );
			}
			else if ( "avg".equals( operation ) ) {
				Double sum = context.getFacets( compensatedSumCollectorKey );
				Long counts = context.getFacets( collectorKey );
				double avg = ( sum / counts );
				extracted = numericDomain.doubleToTerm( avg );
			}
			else {
				Long result = context.getFacets( collectorKey );
				extracted = numericDomain.sortedDocValueToTerm( result );
			}

			F decode = codec.decode( extracted );
			return fromFieldValueConverter.fromDocumentValue( decode, context.fromDocumentValueConvertContext() );
		}
	}

	public static class Factory<F>
			extends AbstractLuceneCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector,
					F,
					AbstractLuceneNumericFieldCodec<F, ?>> {

		private final String operation;

		public Factory(AbstractLuceneNumericFieldCodec<F, ?> codec, String operation) {
			super( codec );
			this.operation = operation;
		}

		@Override
		public FieldMetricAggregationBuilder.TypeSelector create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( codec, scope, field, operation );
		}
	}

	private static class TypeSelector<F> implements FieldMetricAggregationBuilder.TypeSelector {
		private final AbstractLuceneNumericFieldCodec<F, ?> codec;
		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexValueFieldContext<F> field;
		private final String operation;

		private TypeSelector(AbstractLuceneNumericFieldCodec<F, ?> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field,
				String operation) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
			this.operation = operation;
		}

		@Override
		public <T> Builder<F, ?, T> type(Class<T> expectedType, ValueModel valueModel) {
			ProjectionConverter<F, ? extends T> projectionConverter = null;
			if ( !Double.class.isAssignableFrom( expectedType )
					||
					field.type().projectionConverter( valueModel ).valueType().isAssignableFrom( expectedType ) ) {
				projectionConverter = field.type().projectionConverter( valueModel )
						.withConvertedType( expectedType, field );
			}

			return new Builder<>( codec, scope, field,
					projectionConverter,
					operation
			);
		}
	}

	private static class Builder<F, E extends Number, K> extends AbstractBuilder<K>
			implements FieldMetricAggregationBuilder<K> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
		private final String operation;

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter,
				String operation) {
			super( scope, field );
			this.codec = codec;
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.operation = operation;
		}

		@Override
		public LuceneMetricCompensatedSumAggregation<F, E, K> build() {
			return new LuceneMetricCompensatedSumAggregation<>( this );
		}
	}
}
