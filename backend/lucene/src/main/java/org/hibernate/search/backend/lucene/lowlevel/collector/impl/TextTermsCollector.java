/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LongBucket;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.cursors.LongObjectCursor;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class TextTermsCollector extends SimpleCollector implements BaseTermsCollector {

	private final LongHashSet uniqueLeafIndicesForDocument = new LongHashSet();

	private final TextMultiValuesSource valuesSource;
	private final LongObjectHashMap<LongBucket> hashValues = new LongObjectHashMap<>();
	private final LongObjectHashMap<TermCollectorSegmentValue> segmentValues = new LongObjectHashMap<>();
	private final String field;
	private SortedSetDocValues sortedSetValues;

	private final CollectorKey<?, ?>[] keys;
	private final CollectorManager<Collector, ?>[] managers;

	private TextMultiValues values;
	private LeafReaderContext leafReaderContext;

	public TextTermsCollector(String field, TextMultiValuesSource valuesSource,
			CollectorKey<?, ?>[] keys, CollectorManager<Collector, ?>[] managers) {
		this.field = field;
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
				long value = values.nextOrd();
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
	public CollectorKey<?, ?>[] keys() {
		return keys;
	}

	@Override
	public CollectorManager<Collector, ?>[] managers() {
		return managers;
	}

	LongObjectHashMap<LongBucket> segmentValues() {
		return hashValues;
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		initRootSortedSetDocValues( context );
		this.values = valuesSource.getValues( context );
		leafReaderContext = context;
	}

	@Override
	public void finish() throws IOException {
		for ( LongObjectCursor<TermCollectorSegmentValue> value : segmentValues ) {
			long globalOrd = sortedSetValues.lookupTerm( values.lookupOrd( value.key ) );
			LongBucket bucket = hashValues.get( globalOrd );
			if ( bucket == null ) {
				bucket = new LongBucket( globalOrd, value.value.collectors, value.value.count );
				hashValues.put( globalOrd, bucket );
			}
			else {
				bucket.count += value.value.count;
				for ( int i = 0; i < bucket.collectors.length; i++ ) {
					bucket.collectors[i].add( value.value.collectors[i] );
				}
			}
		}
		this.values = null;
		this.segmentValues.clear();
	}

	private void initRootSortedSetDocValues(IndexReaderContext ctx) throws IOException {
		if ( sortedSetValues != null || ctx == null ) {
			return;
		}
		if ( ctx.isTopLevel ) {
			this.sortedSetValues = MultiDocValues.getSortedSetValues( ctx.reader(), field );
		}
		initRootSortedSetDocValues( ctx.parent );
	}
}
