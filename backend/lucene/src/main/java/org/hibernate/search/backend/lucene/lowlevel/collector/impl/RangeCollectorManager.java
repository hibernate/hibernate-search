/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.EffectiveRange;

import org.apache.lucene.search.CollectorManager;

public class RangeCollectorManager<E extends Number> implements CollectorManager<RangeCollector, RangeCollector> {

	private final LongMultiValuesSource valuesSource;
	private final EffectiveRange[] ranges;

	public RangeCollectorManager(LongMultiValuesSource valuesSource, EffectiveRange[] ranges) {
		this.valuesSource = valuesSource;
		this.ranges = ranges;
	}

	@Override
	public RangeCollector newCollector() {
		return new RangeCollector( valuesSource, ranges );
	}

	@Override
	public RangeCollector reduce(Collection<RangeCollector> collection) {
		// TODO: actually reduce:
		return collection.iterator().next();
	}
}
