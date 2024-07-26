/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.SumCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;

public class LuceneAvgNumericFieldAggregation<F, E extends Number, K>
		extends AbstractLuceneMetricNumericFieldAggregation<F, E, K> {

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
}
