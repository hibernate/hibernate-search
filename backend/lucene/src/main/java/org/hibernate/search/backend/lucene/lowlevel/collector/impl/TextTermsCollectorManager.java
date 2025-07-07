/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public class TextTermsCollectorManager
		implements CollectorManager<TextTermsCollector, TermResults> {

	private final TextMultiValuesSource valuesSource;
	private final String field;
	private final CollectorKey<?, ?>[] keys;
	private final CollectorManager<Collector, ?>[] managers;

	public TextTermsCollectorManager(String field, TextMultiValuesSource valuesSource,
			CollectorKey<?, ?>[] keys, CollectorManager<Collector, ?>[] managers) {
		this.field = field;
		this.valuesSource = valuesSource;
		this.keys = keys;
		this.managers = managers;
	}

	@Override
	public TextTermsCollector newCollector() {
		return new TextTermsCollector( field, valuesSource, keys, managers );
	}

	@Override
	public TermResults reduce(Collection<TextTermsCollector> collection) {
		if ( collection.isEmpty() ) {
			return TermResults.EMPTY;
		}
		TermResults results = new TermResults( keys, managers );
		for ( TextTermsCollector collector : collection ) {
			results.merge( collector.segmentValues() );
		}
		return results;
	}
}
