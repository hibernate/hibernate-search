/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.SumCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;

public class LuceneAvgNumericFieldAggregation<F, E extends Number, K>
		extends AbstractLuceneMetricNumericFieldAggregation<F, E, K> {

	public static <F> Factory<F> factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
		return new Factory<>( codec );
	}

	LuceneAvgNumericFieldAggregation(Builder<F, E, K> builder) {
		super( builder );
	}

	@Override
	void fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context) {
		SumCollectorFactory sumCollectorFactory = new SumCollectorFactory( source );
		CountCollectorFactory countCollectorFactory = new CountCollectorFactory( source );
		collectorKey = sumCollectorFactory.getCollectorKey();
		countCollectorKey = countCollectorFactory.getCollectorKey();
		context.requireCollector( sumCollectorFactory );
		context.requireCollector( countCollectorFactory );
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
			return new TypeSelector<>( codec, scope, field, "avg" );
		}
	}

	protected static class Builder<F, E extends Number, K>
			extends AbstractLuceneMetricNumericFieldAggregation.Builder<F, E, K> {

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec,
				LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
			super( codec, scope, field, fromFieldValueConverter );
		}

		@Override
		public AbstractLuceneMetricNumericFieldAggregation<F, E, K> build() {
			return new LuceneAvgNumericFieldAggregation<>( this );
		}
	}
}
