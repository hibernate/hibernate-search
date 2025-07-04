/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.EffectiveRange;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public class RangeCollectorManager implements CollectorManager<RangeCollector, RangeCollector> {

	private final LongMultiValuesSource valuesSource;
	private final EffectiveRange[] ranges;
	private final Collector[][] collectors;
	private final CollectorKey<?, ?>[] keys;
	private final CollectorManager<Collector, ?>[] managers;

	public RangeCollectorManager(LongMultiValuesSource valuesSource, EffectiveRange[] ranges, Collector[][] collectors,
			CollectorKey<?, ?>[] keys, CollectorManager<Collector, ?>[] managers) {
		this.valuesSource = valuesSource;
		this.ranges = ranges;
		this.collectors = collectors;
		this.keys = keys;
		this.managers = managers;
	}

	@Override
	public RangeCollector newCollector() {
		return new RangeCollector( valuesSource, ranges, collectors, keys, managers );
	}

	@Override
	public RangeCollector reduce(Collection<RangeCollector> collection) {
		// TODO: actually reduce:
		return collection.iterator().next();
	}
}
