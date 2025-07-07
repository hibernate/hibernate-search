/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public class RangeResults {

	@SuppressWarnings("unchecked")
	static final RangeResults EMPTY = new RangeResults( new CollectorKey[0], new CollectorManager[0], 0 );

	private final CollectorKey<?, ?>[] collectorKeys;
	private final CollectorManager<Collector, ?>[] managers;

	private final List<Collector>[][] buckets;

	@SuppressWarnings("unchecked")
	RangeResults(CollectorKey<?, ?>[] collectorKeys, CollectorManager<Collector, ?>[] managers, int ranges) {
		this.collectorKeys = collectorKeys;
		this.managers = managers;
		this.buckets = new List[managers.length][];
		for ( int i = 0; i < buckets.length; i++ ) {
			buckets[i] = new List[ranges];
			for ( int j = 0; j < buckets[i].length; j++ ) {
				buckets[i][j] = new ArrayList<>();
			}
		}
	}

	void add(Collector[][] collectors) {
		for ( int collectorIndex = 0; collectorIndex < collectors.length; collectorIndex++ ) {
			for ( int rangeIndex = 0; rangeIndex < collectors[collectorIndex].length; rangeIndex++ ) {
				buckets[collectorIndex][rangeIndex].add( collectors[collectorIndex][rangeIndex] );
			}
		}
	}

	public List<Collector>[][] buckets() {
		return buckets;
	}

	public CollectorKey<?, ?>[] collectorKeys() {
		return collectorKeys;
	}

	public CollectorManager<Collector, ?>[] collectorManagers() {
		return managers;
	}
}
