/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CompensatedSumCollectorFactory;
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

public class LuceneSumCompensatedSumAggregation<F, E extends Number, K>
		extends AbstractLuceneMetricCompensatedSumAggregation<F, E, K> {

	public static <F> Factory<F> factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
		return new Factory<>( codec );
	}

	LuceneSumCompensatedSumAggregation(Builder<F, E, K> builder) {
		super( builder );
	}

	@Override
	void fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context,
			LuceneNumericDomain<E> numericDomain) {
		CompensatedSumCollectorFactory collectorFactory = new CompensatedSumCollectorFactory( source,
				numericDomain::sortedDocValueToDouble );
		compensatedSumCollectorKey = collectorFactory.getCollectorKey();
		context.requireCollector( collectorFactory );
	}

	@Override
	E extractEncoded(AggregationExtractContext context, LuceneNumericDomain<E> numericDomain) {
		Double sum = context.getFacets( compensatedSumCollectorKey );
		return numericDomain.doubleToTerm( sum );
	}

	public static class Factory<F>
			extends AbstractLuceneCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector,
					F,
					AbstractLuceneNumericFieldCodec<F, ?>> {

		protected Factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
			super( codec );
		}

		@Override
		public FieldMetricAggregationBuilder.TypeSelector create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			return new FunctionTypeSelector<>( codec, scope, field );
		}
	}

	protected static class FunctionTypeSelector<F> extends TypeSelector<F>
			implements FieldMetricAggregationBuilder.TypeSelector {

		protected FunctionTypeSelector(AbstractLuceneNumericFieldCodec<F, ?> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( codec, scope, field );
		}

		@Override
		protected <T> Builder<F, ? extends Number, T> getFtBuilder(
				ProjectionConverter<F, ? extends T> projectionConverter) {
			return new Builder<>( codec, scope, field, projectionConverter );
		}
	}

	protected static class Builder<F, E extends Number, K>
			extends AbstractLuceneMetricCompensatedSumAggregation.Builder<F, E, K> {

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec,
				LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
			super( codec, scope, field, fromFieldValueConverter );
		}

		@Override
		public AbstractLuceneMetricCompensatedSumAggregation<F, E, K> build() {
			return new LuceneSumCompensatedSumAggregation<>( this );
		}
	}
}
