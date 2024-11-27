/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;

public class CountCollectorFactory
		implements
		CollectorFactory<AggregationFunctionCollector<Count>, Long, AggregationFunctionCollectorManager<Count>> {

	private final JoiningLongMultiValuesSource source;
	private final CollectorKey<AggregationFunctionCollector<Count>, Long> key = CollectorKey.create();

	public CountCollectorFactory(JoiningLongMultiValuesSource source) {
		this.source = source;
	}

	@Override
	public AggregationFunctionCollectorManager<Count> createCollectorManager(CollectorExecutionContext context) {
		return new AggregationFunctionCollectorManager<>( source, Count::new );
	}

	@Override
	public CollectorKey<AggregationFunctionCollector<Count>, Long> getCollectorKey() {
		return key;
	}
}
