/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.backend.lucene.types.aggregation.impl.BucketOrder;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LongBucket;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.procedures.LongObjectProcedure;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.util.PriorityQueue;

public class TermResults {

	@SuppressWarnings("unchecked")
	static final TermResults EMPTY = new TermResults( new CollectorKey[0], new CollectorManager[0] );

	private final CollectorKey<?, ?>[] collectorKeys;
	private final CollectorManager<Collector, ?>[] managers;

	private final LongObjectHashMap<LongBucket> buckets = new LongObjectHashMap<>();

	TermResults(CollectorKey<?, ?>[] collectorKeys, CollectorManager<Collector, ?>[] managers) {
		this.collectorKeys = collectorKeys;
		this.managers = managers;
	}

	public List<LongBucket> counts(BucketOrder order, int topN, int minDocCount) {
		int size = Math.min( topN, buckets.size() );
		PriorityQueue<LongBucket> pq = new HibernateSearchBucketOrderQueue( order, size );

		buckets.forEach( (LongObjectProcedure<LongBucket>) (key, value) -> {
			if ( value.count >= minDocCount ) {
				pq.insertWithOverflow( value );
			}
		} );

		List<LongBucket> results = new LinkedList<>();
		while ( pq.size() != 0 ) {
			LongBucket popped = pq.pop();
			results.add( 0, popped );
		}

		return results;
	}

	void add(LongObjectHashMap<TermCollectorSegmentValue> segmentValues) {
		for ( var segment : segmentValues ) {
			LongBucket bucket = buckets.get( segment.key );
			if ( bucket == null ) {
				bucket = new LongBucket( segment.key, segment.value.collectors, segment.value.count );
				buckets.put( segment.key, bucket );
			}
			else {
				bucket.add( segment.value.collectors, segment.value.count );
			}
		}
	}

	public void merge(LongObjectHashMap<LongBucket> values) {
		for ( var toadd : values ) {
			LongBucket bucket = buckets.get( toadd.key );
			if ( bucket == null ) {
				bucket = new LongBucket( toadd.key, toadd.value.collectors, toadd.value.count );
				buckets.put( toadd.key, bucket );
			}
			else {
				bucket.add( toadd.value );
			}
		}
	}

	public CollectorKey<?, ?>[] collectorKeys() {
		return collectorKeys;
	}

	public CollectorManager<Collector, ?>[] collectorManagers() {
		return managers;
	}

	private static class HibernateSearchBucketOrderQueue extends PriorityQueue<LongBucket> {

		public HibernateSearchBucketOrderQueue(BucketOrder order, int maxSize) {
			super( maxSize, new ComparatorBasedLessThan( order ) );
		}

		private static class ComparatorBasedLessThan implements LessThan<LongBucket> {
			private final Comparator<LongBucket> comparator;

			public ComparatorBasedLessThan(BucketOrder order) {
				this.comparator = order.toLongBucketComparator();
			}

			@Override
			public boolean lessThan(LongBucket t1, LongBucket t2) {
				return comparator.compare( t1, t2 ) > 0;
			}
		}
	}
}
