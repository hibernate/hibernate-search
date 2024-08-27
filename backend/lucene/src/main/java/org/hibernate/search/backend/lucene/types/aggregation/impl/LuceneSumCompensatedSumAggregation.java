/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CompensatedSumCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

public class LuceneSumCompensatedSumAggregation<F, E extends Number, K>
		extends AbstractLuceneMetricCompensatedSumAggregation<F, E, K> {

	public static <F> Factory<F> factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
		return new Factory<>( codec, "sum" );
	}

	LuceneSumCompensatedSumAggregation(AbstractLuceneMetricCompensatedSumAggregation.Builder<F, E, K> builder) {
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
}
