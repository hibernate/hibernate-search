/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;

public class MaxCollectorFactory
		implements
		CollectorFactory<AggregationFunctionCollector<Max>, Long, AggregationFunctionCollectorManager<Max>> {

	private final JoiningLongMultiValuesSource source;
	private final CollectorKey<AggregationFunctionCollector<Max>, Long> key = CollectorKey.create();

	public MaxCollectorFactory(JoiningLongMultiValuesSource source) {
		this.source = source;
	}

	@Override
	public AggregationFunctionCollectorManager<Max> createCollectorManager(CollectorExecutionContext context) {
		return new AggregationFunctionCollectorManager<>( source, Max::new );
	}

	@Override
	public CollectorKey<AggregationFunctionCollector<Max>, Long> getCollectorKey() {
		return key;
	}
}
