/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.EffectiveRange;

import org.apache.lucene.search.CollectorManager;

public class RangeCollectorFactory
		implements CollectorFactory<RangeCollector, RangeResults, RangeCollectorManager> {

	public static CollectorFactory<RangeCollector, RangeResults, RangeCollectorManager> instance(
			LongMultiValuesSource valuesSource, EffectiveRange[] ranges, List<CollectorFactory<?, ?, ?>> collectorFactories) {
		return new RangeCollectorFactory( valuesSource, ranges, collectorFactories );
	}

	public final CollectorKey<RangeCollector, RangeResults> key = CollectorKey.create();
	private final LongMultiValuesSource valuesSource;
	private final EffectiveRange[] ranges;
	private final List<CollectorFactory<?, ?, ?>> collectorFactories;

	public RangeCollectorFactory(LongMultiValuesSource valuesSource, EffectiveRange[] ranges,
			List<CollectorFactory<?, ?, ?>> collectorFactories) {
		this.valuesSource = valuesSource;
		this.ranges = ranges;
		this.collectorFactories = collectorFactories;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public RangeCollectorManager createCollectorManager(CollectorExecutionContext context) throws IOException {
		CollectorKey<?, ?>[] keys = new CollectorKey<?, ?>[collectorFactories.size()];
		var managers = new CollectorManager[collectorFactories.size()];
		int index = 0;
		for ( CollectorFactory<?, ?, ?> collectorFactory : collectorFactories ) {
			CollectorManager<?, ?> collectorManager = collectorFactory.createCollectorManager( context );
			keys[index] = collectorFactory.getCollectorKey();
			managers[index] = collectorManager;
			index++;
		}
		return new RangeCollectorManager( valuesSource, ranges, keys, managers );
	}

	@Override
	public CollectorKey<RangeCollector, RangeResults> getCollectorKey() {
		return key;
	}
}
