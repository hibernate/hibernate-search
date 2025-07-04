/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;

public class CountDistinctValuesCollectorFactory
		implements
		CollectorFactory<AggregationFunctionCollector<CountDistinctValues>,
				Long,
				AggregationFunctionCollectorManager<CountDistinctValues>> {

	private final JoiningLongMultiValuesSource source;
	private final CollectorKey<AggregationFunctionCollector<CountDistinctValues>, Long> key = CollectorKey.create();

	public CountDistinctValuesCollectorFactory(JoiningLongMultiValuesSource source) {
		this.source = source;
	}

	@Override
	public AggregationFunctionCollectorManager<CountDistinctValues> createCollectorManager(CollectorExecutionContext context) {
		return new AggregationFunctionCollectorManager<>( source, CountDistinctValues::new );
	}

	@Override
	public CollectorKey<AggregationFunctionCollector<CountDistinctValues>, Long> getCollectorKey() {
		return key;
	}
}
