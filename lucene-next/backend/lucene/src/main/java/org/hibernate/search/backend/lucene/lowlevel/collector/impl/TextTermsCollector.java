/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;
import org.hibernate.search.backend.lucene.types.aggregation.impl.BucketOrder;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LongBucket;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.cursors.LongIntCursor;
import com.carrotsearch.hppc.procedures.LongIntProcedure;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.PriorityQueue;

public class TextTermsCollector extends SimpleCollector {

	private final LongHashSet uniqueLeafIndicesForDocument = new LongHashSet();

	private final TextMultiValuesSource valuesSource;
	private final LongIntMap hashCounts = new LongIntHashMap();
	private final LongIntMap segmentCounts = new LongIntHashMap();
	private final String field;
	private SortedSetDocValues sortedSetValues;

	private TextMultiValues values;

	public TextTermsCollector(String field, TextMultiValuesSource valuesSource) {
		this.field = field;
		this.valuesSource = valuesSource;
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
					segmentCounts.addTo( value, 1 );
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
			buckets.addFirst( popped );
		}

		return buckets;
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		initRootSortedSetDocValues( context );
		this.values = valuesSource.getValues( context );
	}

	public void finish() throws IOException {
		for ( LongIntCursor hashCount : segmentCounts ) {
			hashCounts.addTo( sortedSetValues.lookupTerm( values.lookupOrd( hashCount.key ) ), hashCount.value );
		}
		this.values = null;
		this.segmentCounts.clear();
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
