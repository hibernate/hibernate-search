/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.EffectiveRange;

public class RangeCollectorFactory<E extends Number>
		implements CollectorFactory<RangeCollector, RangeCollector, RangeCollectorManager<E>> {

	public static <E extends Number> CollectorFactory<RangeCollector, RangeCollector, RangeCollectorManager<E>> instance(
			LongMultiValuesSource valuesSource, EffectiveRange[] ranges) {
		return new RangeCollectorFactory<>( valuesSource, ranges );
	}

	public final CollectorKey<RangeCollector, RangeCollector> key = CollectorKey.create();
	private final LongMultiValuesSource valuesSource;
	private final EffectiveRange[] ranges;

	public RangeCollectorFactory(LongMultiValuesSource valuesSource, EffectiveRange[] ranges) {
		this.valuesSource = valuesSource;
		this.ranges = ranges;
	}

	@Override
	public RangeCollectorManager<E> createCollectorManager(CollectorExecutionContext context) {
		return new RangeCollectorManager<>( valuesSource, ranges );
	}

	@Override
	public CollectorKey<RangeCollector, RangeCollector> getCollectorKey() {
		return key;
	}
}
