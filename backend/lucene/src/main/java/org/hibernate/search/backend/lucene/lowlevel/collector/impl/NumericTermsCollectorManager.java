/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public class NumericTermsCollectorManager
		implements CollectorManager<NumericTermsCollector, TermResults> {

	private final LongMultiValuesSource valuesSource;
	private final CollectorKey<?, ?>[] keys;
	private final CollectorManager<Collector, ?>[] managers;

	public NumericTermsCollectorManager(LongMultiValuesSource valuesSource,
			CollectorKey<?, ?>[] keys, CollectorManager<Collector, ?>[] managers) {
		this.valuesSource = valuesSource;
		this.keys = keys;
		this.managers = managers;
	}

	@Override
	public NumericTermsCollector newCollector() {
		return new NumericTermsCollector( valuesSource, keys, managers );
	}

	@Override
	public TermResults reduce(Collection<NumericTermsCollector> collection) {
		if ( collection.isEmpty() ) {
			return TermResults.EMPTY;
		}
		TermResults results = new TermResults( keys, managers );
		for ( NumericTermsCollector collector : collection ) {
			results.add( collector.segmentValues() );
		}
		return results;
	}
}
