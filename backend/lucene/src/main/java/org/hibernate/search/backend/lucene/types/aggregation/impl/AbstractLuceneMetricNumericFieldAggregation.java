/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.AggregationFunctionCollector;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.Count;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.cfg.spi.NumberUtils;
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
	private final AbstractLuceneNumericFieldCodec<F, E> codec;
	private final LuceneNumericDomain<E> numericDomain;
	private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

	protected CollectorKey<?, Long> collectorKey;

	// Supplementary collector used by the avg function
	protected CollectorKey<AggregationFunctionCollector<Count>, Long> countCollectorKey;

	AbstractLuceneMetricNumericFieldAggregation(Builder<F, E, K> builder) {
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
		@SuppressWarnings("unchecked")
		public K extract(AggregationExtractContext context) {
			Long collector = context.getFacets( collectorKey );
			if ( countCollectorKey != null ) {
				Long counts = context.getFacets( countCollectorKey );
				Double avg = ( (double) collector / counts );
				if ( fromFieldValueConverter == null ) {
					return (K) avg;
				}

				collector = NumberUtils.toLong( avg );
			}

			if ( fromFieldValueConverter == null ) {
				Double decode = collector.doubleValue();
				return (K) decode;
			}

			E e = numericDomain.sortedDocValueToTerm( collector );
			F decode = codec.decode( e );
			return fromFieldValueConverter.fromDocumentValue( decode, context.fromDocumentValueConvertContext() );
		}
	}

	protected abstract static class TypeSelector<F> implements FieldMetricAggregationBuilder.TypeSelector {
		protected final AbstractLuceneNumericFieldCodec<F, ?> codec;
		protected final LuceneSearchIndexScope<?> scope;
		protected final LuceneSearchIndexValueFieldContext<F> field;

		protected TypeSelector(AbstractLuceneNumericFieldCodec<F, ?> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
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

			return getFtBuilder( projectionConverter );
		}

		protected abstract <T> Builder<F, ? extends Number, T> getFtBuilder(
				ProjectionConverter<F, ? extends T> projectionConverter);
	}

	protected abstract static class Builder<F, E extends Number, K> extends AbstractBuilder<K>
			implements FieldMetricAggregationBuilder<K> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
			super( scope, field );
			this.codec = codec;
			this.fromFieldValueConverter = fromFieldValueConverter;
		}
	}
}
