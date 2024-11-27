/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;

public class CountDistinctCollectorFactory
		implements
		CollectorFactory<AggregationFunctionCollector<CountDistinct>,
				Long,
				AggregationFunctionCollectorManager<CountDistinct>> {

	private final JoiningLongMultiValuesSource source;
	private final CollectorKey<AggregationFunctionCollector<CountDistinct>, Long> key = CollectorKey.create();

	public CountDistinctCollectorFactory(JoiningLongMultiValuesSource source) {
		this.source = source;
	}

	@Override
	public AggregationFunctionCollectorManager<CountDistinct> createCollectorManager(CollectorExecutionContext context) {
		return new AggregationFunctionCollectorManager<>( source, CountDistinct::new );
	}

	@Override
	public CollectorKey<AggregationFunctionCollector<CountDistinct>, Long> getCollectorKey() {
		return key;
	}
}
