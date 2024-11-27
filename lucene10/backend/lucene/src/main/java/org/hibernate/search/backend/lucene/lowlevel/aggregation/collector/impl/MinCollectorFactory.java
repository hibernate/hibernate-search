/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;

public class MinCollectorFactory
		implements
		CollectorFactory<AggregationFunctionCollector<Min>, Long, AggregationFunctionCollectorManager<Min>> {

	private final JoiningLongMultiValuesSource source;
	private final CollectorKey<AggregationFunctionCollector<Min>, Long> key = CollectorKey.create();

	public MinCollectorFactory(JoiningLongMultiValuesSource source) {
		this.source = source;
	}

	@Override
	public AggregationFunctionCollectorManager<Min> createCollectorManager(CollectorExecutionContext context) {
		return new AggregationFunctionCollectorManager<>( source, Min::new );
	}

	@Override
	public CollectorKey<AggregationFunctionCollector<Min>, Long> getCollectorKey() {
		return key;
	}
}
