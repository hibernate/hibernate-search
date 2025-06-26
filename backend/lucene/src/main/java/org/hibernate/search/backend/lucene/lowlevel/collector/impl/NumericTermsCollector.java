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
import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.procedures.LongIntProcedure;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.PriorityQueue;

public class NumericTermsCollector extends SimpleCollector {

	private final LongHashSet uniqueLeafIndicesForDocument = new LongHashSet();

	private final LongMultiValuesSource valuesSource;
	private final LongIntMap hashCounts = new LongIntHashMap();
	private LongMultiValues values;

	public NumericTermsCollector(LongMultiValuesSource valuesSource) {
		this.valuesSource = valuesSource;
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
					hashCounts.addTo( value, 1 );
				}
			}
		}
	}

	public List<LongBucket> counts(BucketOrder order, int topN, int minDocCount) {
		int size = Math.min( topN, hashCounts.size() );
		PriorityQueue<LongBucket> pq = new HibernateSearchBucketOrderQueue( order, size );

		hashCounts.forEach( (LongIntProcedure) (key, value) -> {
			if ( value >= minDocCount ) {
				pq.insertWithOverflow( new LongBucket( key, value ) );
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

	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		values = valuesSource.getValues( context );
	}

	public void finish() {
		values = null;
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

}
