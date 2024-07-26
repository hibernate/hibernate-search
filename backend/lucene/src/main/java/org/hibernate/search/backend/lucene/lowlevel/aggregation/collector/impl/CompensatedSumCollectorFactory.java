/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;

public class CompensatedSumCollectorFactory
		implements
		CollectorFactory<DoubleAggregationFunctionCollector<CompensatedSum>,
				Double,
				DoubleAggregationFunctionCollectorManager<CompensatedSum>> {

	private final JoiningLongMultiValuesSource source;
	private final CollectorKey<DoubleAggregationFunctionCollector<CompensatedSum>, Double> key = CollectorKey.create();
	private final Function<Long, Double> longToDouble;

	public CompensatedSumCollectorFactory(JoiningLongMultiValuesSource source, Function<Long, Double> longToDouble) {
		this.source = source;
		this.longToDouble = longToDouble;
	}

	@Override
	public DoubleAggregationFunctionCollectorManager<CompensatedSum> createCollectorManager(CollectorExecutionContext context) {
		return new DoubleAggregationFunctionCollectorManager<>( source, CompensatedSum::new, longToDouble );
	}

	@Override
	public CollectorKey<DoubleAggregationFunctionCollector<CompensatedSum>, Double> getCollectorKey() {
		return key;
	}
}
