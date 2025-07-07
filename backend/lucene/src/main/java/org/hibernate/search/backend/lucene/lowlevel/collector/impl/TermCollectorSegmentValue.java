/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.LeafCollector;

class TermCollectorSegmentValue {
	final Collector[] collectors;
	final LeafCollector[] leafCollectors;
	long count = 0L;

	TermCollectorSegmentValue(CollectorManager<Collector, ?>[] managers, LeafReaderContext leafReaderContext)
			throws IOException {
		this.collectors = new Collector[managers.length];
		this.leafCollectors = new LeafCollector[managers.length];
		for ( int i = 0; i < managers.length; i++ ) {
			collectors[i] = managers[i].newCollector();
			leafCollectors[i] = collectors[i].getLeafCollector( leafReaderContext );
		}
	}

	void collect(int doc) throws IOException {
		count++;
		for ( LeafCollector collector : leafCollectors ) {
			collector.collect( doc );
		}
	}

	void resetLeafCollectors(LeafReaderContext leafReaderContext) throws IOException {
		for ( int i = 0; i < leafCollectors.length; i++ ) {
			leafCollectors[i] = collectors[i].getLeafCollector( leafReaderContext );
		}
	}
}
