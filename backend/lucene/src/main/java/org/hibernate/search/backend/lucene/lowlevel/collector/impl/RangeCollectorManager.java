/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.EffectiveRange;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public class RangeCollectorManager implements CollectorManager<RangeCollector, RangeResults> {

	private final LongMultiValuesSource valuesSource;
	private final EffectiveRange[] ranges;
	private final CollectorKey<?, ?>[] keys;
	private final CollectorManager<Collector, ?>[] managers;

	public RangeCollectorManager(LongMultiValuesSource valuesSource, EffectiveRange[] ranges,
			CollectorKey<?, ?>[] keys, CollectorManager<Collector, ?>[] managers) {
		this.valuesSource = valuesSource;
		this.ranges = ranges;
		this.keys = keys;
		this.managers = managers;
	}

	@Override
	public RangeCollector newCollector() throws IOException {
		Collector[][] collectors = new Collector[keys.length][];
		int index = 0;
		for ( CollectorManager<Collector, ?> manager : managers ) {
			Collector[] c = new Collector[ranges.length];
			collectors[index] = c;
			for ( int j = 0; j < c.length; j++ ) {
				c[j] = manager.newCollector();
			}
			index++;
		}
		return new RangeCollector( valuesSource, ranges, collectors, keys );
	}

	@Override
	public RangeResults reduce(Collection<RangeCollector> collection) {
		if ( collection.isEmpty() ) {
			return RangeResults.EMPTY;
		}
		RangeResults results = new RangeResults( keys, managers, ranges.length );
		for ( RangeCollector collector : collection ) {
			results.add( collector.collectors() );
		}
		return results;
	}
}
