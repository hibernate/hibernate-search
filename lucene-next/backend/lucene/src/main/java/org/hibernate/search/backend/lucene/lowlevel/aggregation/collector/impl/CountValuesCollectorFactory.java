/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;

public class CountValuesCollectorFactory
		implements
		CollectorFactory<AggregationFunctionCollector<CountValues>, Long, AggregationFunctionCollectorManager<CountValues>> {

	private final JoiningLongMultiValuesSource source;
	private final CollectorKey<AggregationFunctionCollector<CountValues>, Long> key = CollectorKey.create();

	public CountValuesCollectorFactory(JoiningLongMultiValuesSource source) {
		this.source = source;
	}

	@Override
	public AggregationFunctionCollectorManager<CountValues> createCollectorManager(CollectorExecutionContext context) {
		return new AggregationFunctionCollectorManager<>( source, CountValues::new );
	}

	@Override
	public CollectorKey<AggregationFunctionCollector<CountValues>, Long> getCollectorKey() {
		return key;
	}
}
