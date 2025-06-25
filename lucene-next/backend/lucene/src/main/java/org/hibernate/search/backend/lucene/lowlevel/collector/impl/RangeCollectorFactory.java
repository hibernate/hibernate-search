/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.aggregation.impl.EffectiveRange;

public class RangeCollectorFactory
		implements CollectorFactory<RangeCollector, RangeCollector, RangeCollectorManager> {

	public static CollectorFactory<RangeCollector, RangeCollector, RangeCollectorManager> instance(
			LongMultiValuesSource valuesSource, EffectiveRange[] ranges) {
		return new RangeCollectorFactory( valuesSource, ranges );
	}

	public final CollectorKey<RangeCollector, RangeCollector> key = CollectorKey.create();
	private final LongMultiValuesSource valuesSource;
	private final EffectiveRange[] ranges;

	public RangeCollectorFactory(LongMultiValuesSource valuesSource, EffectiveRange[] ranges) {
		this.valuesSource = valuesSource;
		this.ranges = ranges;
	}

	@Override
	public RangeCollectorManager createCollectorManager(CollectorExecutionContext context) {
		return new RangeCollectorManager( valuesSource, ranges );
	}

	@Override
	public CollectorKey<RangeCollector, RangeCollector> getCollectorKey() {
		return key;
	}
}
