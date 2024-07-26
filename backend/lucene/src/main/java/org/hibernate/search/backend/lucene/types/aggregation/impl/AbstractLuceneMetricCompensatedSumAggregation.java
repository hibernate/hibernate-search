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
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.AssertionFailure;

/**
 * @param <F> The type of field values.
 * @param <K> The type of returned value. It can be {@code F}, {@link Double}
 * or a different type if value converters are used.
 */
public abstract class AbstractLuceneMetricCompensatedSumAggregation<F, E extends Number, K>
		extends AbstractLuceneNestableAggregation<K> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final AbstractLuceneNumericFieldCodec<F, E> codec;
	private final LuceneNumericDomain<E> numericDomain;
	private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

	protected CollectorKey<?, Long> collectorKey;
	protected CollectorKey<DoubleAggregationFunctionCollector<CompensatedSum>, Double> compensatedSumCollectorKey;

	AbstractLuceneMetricCompensatedSumAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.indexNames = builder.scope.hibernateSearchIndexNames();
		this.absoluteFieldPath = builder.field.absolutePath();
		this.codec = builder.codec;
		this.numericDomain = codec.getDomain();
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
	}

	@Override
	public Extractor<K> request(AggregationRequestContext context) {
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromField(
				absoluteFieldPath, createNestedDocsProvider( context )
		);
		fillCollectors( source, context, numericDomain );
		return new LuceneNumericMetricFieldAggregationExtraction();
	}

	abstract void fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context,
			LuceneNumericDomain<E> numericDomain);

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	private class LuceneNumericMetricFieldAggregationExtraction implements Extractor<K> {

		@Override
		@SuppressWarnings("unchecked")
		public K extract(AggregationExtractContext context) {
			E extracted = extractEncoded( context, numericDomain );
			F decode = codec.decode( extracted );

			if ( fromFieldValueConverter == null ) {
				return (K) decode;
			}
			return fromFieldValueConverter.fromDocumentValue( decode, context.fromDocumentValueConvertContext() );
		}
	}

	abstract E extractEncoded(AggregationExtractContext context, LuceneNumericDomain<E> numericDomain);

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

	protected static class Builder<F, E extends Number, K> extends AbstractBuilder<K>
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
		public AbstractLuceneMetricCompensatedSumAggregation<F, E, K> build() {
			if ( "sum".equals( operation ) ) {
				return new LuceneSumCompensatedSumAggregation<>( this );
			}
			else if ( "avg".equals( operation ) ) {
				return new LuceneAvgCompensatedSumAggregation<>( this );
			}
			else {
				throw new AssertionFailure( "Aggregation operation not supported: " + operation );
			}
		}
	}
}
