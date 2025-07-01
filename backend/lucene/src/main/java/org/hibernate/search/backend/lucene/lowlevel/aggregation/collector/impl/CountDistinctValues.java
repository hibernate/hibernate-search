/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.util.BitSet;

import com.carrotsearch.hppc.LongHashSet;

/**
 * <p>
 * The algorithm to collect distinct elements is inspired by {@code org.apache.lucene.facet.LongValueFacetCounts}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
public class CountDistinctValues implements AggregationFunction<CountDistinctValues> {

	private final BitSet counts = new BitSet( 1024 );
	private final LongHashSet hashCounts = new LongHashSet();

	@Override
	public void apply(long value) {
		if ( value >= 0 && value < counts.size() ) {
			counts.set( (int) value );
		}
		else {
			hashCounts.add( value );
		}
	}

	@Override
	public void merge(AggregationFunction<CountDistinctValues> sibling) {
		CountDistinctValues other = sibling.implementation();
		counts.or( other.counts );
		hashCounts.addAll( other.hashCounts );
	}

	@Override
	public Long result() {
		return (long) counts.cardinality() + hashCounts.size();
	}

	@Override
	public CountDistinctValues implementation() {
		return this;
	}
}
