/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.cursors.LongObjectCursor;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class NumericTermsCollector extends SimpleCollector implements BaseTermsCollector {

	private final LongHashSet uniqueLeafIndicesForDocument = new LongHashSet();

	private final LongMultiValuesSource valuesSource;
	private final LongObjectHashMap<TermCollectorSegmentValue> segmentValues = new LongObjectHashMap<>();

	private final CollectorKey<?, ?>[] keys;
	private final CollectorManager<Collector, ?>[] managers;

	private LongMultiValues values;
	private LeafReaderContext leafReaderContext;

	public NumericTermsCollector(LongMultiValuesSource valuesSource, CollectorKey<?, ?>[] keys,
			CollectorManager<Collector, ?>[] managers) {
		this.valuesSource = valuesSource;
		this.keys = keys;
		this.managers = managers;
	}

	@Override
	public void collect(int doc) throws IOException {
		if ( values.advanceExact( doc ) ) {
			// or just recreate it on each document?
			uniqueLeafIndicesForDocument.clear();

			while ( values.hasNextValue() ) {
				// Each document must be counted only once per range.
				long value = values.nextValue();
				if ( uniqueLeafIndicesForDocument.add( value ) ) {
					TermCollectorSegmentValue segmentValue = segmentValues.get( value );
					if ( segmentValue == null ) {
						segmentValue = new TermCollectorSegmentValue( managers, leafReaderContext );
						segmentValues.put( value, segmentValue );
					}
					segmentValue.collect( doc );
				}
			}
		}
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.values = valuesSource.getValues( context );
		this.leafReaderContext = context;
		for ( LongObjectCursor<TermCollectorSegmentValue> value : segmentValues ) {
			value.value.resetLeafCollectors( context );
		}
	}

	@Override
	public void finish() {
		values = null;
	}

	@Override
	public CollectorKey<?, ?>[] keys() {
		return keys;
	}

	@Override
	public CollectorManager<Collector, ?>[] managers() {
		return managers;
	}

	LongObjectHashMap<TermCollectorSegmentValue> segmentValues() {
		return segmentValues;
	}

}
