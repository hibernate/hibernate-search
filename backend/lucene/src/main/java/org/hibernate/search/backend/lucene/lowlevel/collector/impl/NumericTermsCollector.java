/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.aggregation.impl.BucketOrder;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LongBucket;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.cursors.LongObjectCursor;
import com.carrotsearch.hppc.procedures.LongObjectProcedure;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.PriorityQueue;

public class NumericTermsCollector extends SimpleCollector implements BaseTermsCollector {

	private final LongHashSet uniqueLeafIndicesForDocument = new LongHashSet();

	private final LongMultiValuesSource valuesSource;
	private final LongObjectHashMap<SegmentValue> segmentValues = new LongObjectHashMap<>();

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
					SegmentValue segmentValue = segmentValues.get( value );
					if ( segmentValue == null ) {
						segmentValue = new SegmentValue( managers );
						segmentValues.put( value, segmentValue );
					}
					segmentValue.collect( doc );
				}
			}
		}
	}

	public List<LongBucket> counts(BucketOrder order, int topN, int minDocCount) {
		int size = Math.min( topN, segmentValues.size() );
		PriorityQueue<LongBucket> pq = new HibernateSearchBucketOrderQueue( order, size );

		segmentValues.forEach( (LongObjectProcedure<SegmentValue>) (key, value) -> {
			if ( value.count >= minDocCount ) {
				pq.insertWithOverflow( new LongBucket( key, value.collectors, value.count ) );
			}
		} );

		List<LongBucket> buckets = new LinkedList<>();
		while ( pq.size() != 0 ) {
			LongBucket popped = pq.pop();
			buckets.add( 0, popped );
		}

		return buckets;
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.values = valuesSource.getValues( context );
		this.leafReaderContext = context;
		for ( LongObjectCursor<SegmentValue> value : segmentValues ) {
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

	private static class HibernateSearchBucketOrderQueue extends PriorityQueue<LongBucket> {
		private final Comparator<LongBucket> comparator;

		public HibernateSearchBucketOrderQueue(BucketOrder order, int maxSize) {
			super( maxSize );
			this.comparator = order.toLongBucketComparator();
		}

		@Override
		protected boolean lessThan(LongBucket t1, LongBucket t2) {
			return comparator.compare( t1, t2 ) > 0;
		}
	}

	private class SegmentValue {
		final Collector[] collectors;
		final LeafCollector[] leafCollectors;
		long count = 0L;

		SegmentValue(CollectorManager<Collector, ?>[] managers) throws IOException {
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

}
