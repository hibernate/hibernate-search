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
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

public class LuceneAvgCompensatedSumAggregation<F, E extends Number, K>
		extends AbstractLuceneMetricCompensatedSumAggregation<F, E, K> {

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
}
