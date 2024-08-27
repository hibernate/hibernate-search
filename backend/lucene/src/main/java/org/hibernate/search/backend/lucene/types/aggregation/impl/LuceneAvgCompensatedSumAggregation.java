/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CompensatedSumCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

public class LuceneAvgCompensatedSumAggregation<F, E extends Number, K>
		extends AbstractLuceneMetricCompensatedSumAggregation<F, E, K> {

	public static <F> Factory<F> factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
		return new Factory<>( codec, "avg" );
	}

	LuceneAvgCompensatedSumAggregation(Builder<F, E, K> builder) {
		super( builder );
	}

	@Override
	void fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context,
			LuceneNumericDomain<E> numericDomain) {
		CompensatedSumCollectorFactory sumCollectorFactory = new CompensatedSumCollectorFactory( source,
				numericDomain::sortedDocValueToDouble );
		compensatedSumCollectorKey = sumCollectorFactory.getCollectorKey();
		context.requireCollector( sumCollectorFactory );

		CountCollectorFactory countCollectorFactory = new CountCollectorFactory( source );
		collectorKey = countCollectorFactory.getCollectorKey();
		context.requireCollector( countCollectorFactory );
	}

	@Override
	E extractEncoded(AggregationExtractContext context, LuceneNumericDomain<E> numericDomain) {
		Double sum = context.getFacets( compensatedSumCollectorKey );
		Long counts = context.getFacets( collectorKey );
		double avg = ( sum / counts );
		return numericDomain.doubleToTerm( avg );
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
			return new LuceneAvgCompensatedSumAggregation<>( this );
		}
	}
}
