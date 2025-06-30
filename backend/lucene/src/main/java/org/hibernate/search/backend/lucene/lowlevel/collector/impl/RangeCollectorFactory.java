/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.EffectiveRange;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public class RangeCollectorFactory<E extends Number>
		implements CollectorFactory<RangeCollector, RangeCollector, RangeCollectorManager<E>> {

	public static <E extends Number> CollectorFactory<RangeCollector, RangeCollector, RangeCollectorManager<E>> instance(
			LongMultiValuesSource valuesSource, EffectiveRange[] ranges, List<CollectorFactory<?, ?, ?>> collectorFactories) {
		return new RangeCollectorFactory<>( valuesSource, ranges, collectorFactories );
	}

	public final CollectorKey<RangeCollector, RangeCollector> key = CollectorKey.create();
	private final LongMultiValuesSource valuesSource;
	private final EffectiveRange[] ranges;
	private final List<CollectorFactory<?, ?, ?>> collectorFactories;

	public RangeCollectorFactory(LongMultiValuesSource valuesSource, EffectiveRange[] ranges,
			List<CollectorFactory<?, ?, ?>> collectorFactories) {
		this.valuesSource = valuesSource;
		this.ranges = ranges;
		this.collectorFactories = collectorFactories;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public RangeCollectorManager<E> createCollectorManager(CollectorExecutionContext context) throws IOException {
		Collector[][] collectors = new Collector[collectorFactories.size()][];
		CollectorKey<?, ?>[] keys = new CollectorKey<?, ?>[collectorFactories.size()];
		var managers = new CollectorManager[collectorFactories.size()];
		int index = 0;
		for ( CollectorFactory<?, ?, ?> collectorFactory : collectorFactories ) {
			CollectorManager<?, ?> collectorManager = collectorFactory.createCollectorManager( context );
			keys[index] = collectorFactory.getCollectorKey();
			managers[index] = collectorManager;
			Collector[] c = new Collector[ranges.length];
			collectors[index] = c;
			for ( int i = 0; i < c.length; i++ ) {
				c[i] = collectorManager.newCollector();
			}
			index++;
		}
		return new RangeCollectorManager<>( valuesSource, ranges, collectors, keys, managers );
	}

	@Override
	public CollectorKey<RangeCollector, RangeCollector> getCollectorKey() {
		return key;
	}
}
