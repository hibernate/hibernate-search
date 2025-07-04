/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.EffectiveRange;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.cursors.IntCursor;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class RangeCollector extends SimpleCollector {

	private final LongMultiValuesSource valuesSource;
	private final long[] boundaries;
	private final IntArrayList[] countsPerBoundaries;

	private final Collector[][] collectors;
	private final CollectorKey<?, ?>[] keys;
	private final LeafCollector[][] leafCollectors;
	private final CollectorManager<Collector, ?>[] managers;

	private LongMultiValues values;

	public RangeCollector(LongMultiValuesSource valuesSource, EffectiveRange[] ranges, Collector[][] collectors,
			CollectorKey<?, ?>[] keys, CollectorManager<Collector, ?>[] managers) {
		this.valuesSource = valuesSource;
		this.collectors = collectors;
		this.keys = keys;
		this.managers = managers;

		// Maps all range inclusive endpoints to int flags; 1
		// = start of interval, 2 = end of interval.  We need to
		// track the start vs end case separately because if a
		// given point is both, then it must be its own
		// elementary interval:
		LongIntMap endsMap = new LongIntHashMap();

		endsMap.put( Long.MIN_VALUE, 1 );
		endsMap.put( Long.MAX_VALUE, 2 );

		for ( EffectiveRange range : ranges ) {
			long min = range.min();
			long max = range.max();
			int cur = endsMap.get( min );
			if ( cur == 0 ) {
				endsMap.put( min, 1 );
			}
			else {
				endsMap.put( min, cur | 1 );
			}
			cur = endsMap.get( max );
			if ( cur == 0 ) {
				endsMap.put( max, 2 );
			}
			else {
				endsMap.put( max, cur | 2 );
			}
		}

		LongArrayList endsList = new LongArrayList( endsMap.keys() );
		Arrays.sort( endsList.buffer, 0, endsList.elementsCount );

		// Build elementaryIntervals (a 1D Venn diagram):
		List<EffectiveRange> elementaryIntervals = new ArrayList<>();
		int upto0 = 1;
		long v = endsList.get( 0 );
		long prev;
		if ( endsMap.get( v ) == 3 ) {
			elementaryIntervals.add( new EffectiveRange( v, v ) );
			prev = v + 1;
		}
		else {
			prev = v;
		}

		while ( upto0 < endsList.size() ) {
			v = endsList.get( upto0 );
			int flags = endsMap.get( v );
			if ( flags == 3 ) {
				// This point is both an end and a start; we need to
				// separate it:
				if ( v > prev ) {
					elementaryIntervals.add( new EffectiveRange( prev, v - 1 ) );
				}
				elementaryIntervals.add( new EffectiveRange( v, v ) );
				prev = v + 1;
			}
			else if ( flags == 1 ) {
				// This point is only the start of an interval;
				// attach it to next interval:
				if ( v > prev ) {
					elementaryIntervals.add( new EffectiveRange( prev, v - 1 ) );
				}
				prev = v;
			}
			else {
				assert flags == 2;
				// This point is only the end of an interval; attach
				// it to last interval:
				elementaryIntervals.add( new EffectiveRange( prev, v ) );
				prev = v + 1;
			}
			upto0++;
		}

		// Set boundaries (ends of each elementary interval):
		boundaries = new long[elementaryIntervals.size()];
		countsPerBoundaries = new IntArrayList[boundaries.length];
		for ( int i = 0; i < boundaries.length; i++ ) {
			EffectiveRange interval = elementaryIntervals.get( i );
			boundaries[i] = interval.max();
			IntArrayList list = new IntArrayList();
			countsPerBoundaries[i] = list;
			for ( int j = 0; j < ranges.length; j++ ) {
				if ( interval.min() >= ranges[j].min() && interval.max() <= ranges[j].max() ) {
					list.add( j );
				}
			}
		}

		leafCollectors = new LeafCollector[keys.length][];
		for ( int i = 0; i < leafCollectors.length; i++ ) {
			leafCollectors[i] = new LeafCollector[ranges.length];
		}
	}

	private void processLeafWithIndex(int index, int doc) throws IOException {
		for ( IntCursor cursor : countsPerBoundaries[index] ) {
			for ( int i = 0; i < keys.length; i++ ) {
				leafCollectors[i][cursor.value].collect( doc );
			}
		}
	}

	private int findLeafIndex(long v) {
		// Binary search to find matched elementary range; we
		// are guaranteed to find a match because the last
		// boundary is Long.MAX_VALUE:
		int lo = 0;
		int hi = boundaries.length - 1;
		while ( true ) {
			int mid = ( lo + hi ) >>> 1;
			if ( v <= boundaries[mid] ) {
				if ( mid == 0 ) {
					return 0;
				}
				else {
					hi = mid - 1;
				}
			}
			else if ( v > boundaries[mid + 1] ) {
				lo = mid + 1;
			}
			else {
				return mid + 1;
			}
		}
	}

	@Override
	public void collect(int doc) throws IOException {
		if ( values.advanceExact( doc ) ) {
			IntHashSet uniqueLeafIndicesForDocument = new IntHashSet();
			while ( values.hasNextValue() ) {
				// Each document must be counted only once per range.
				int leafIndex = findLeafIndex( values.nextValue() );
				if ( uniqueLeafIndicesForDocument.add( leafIndex ) ) {
					processLeafWithIndex( leafIndex, doc );
				}
			}
		}
	}

	public Collector[][] collectors() {
		return collectors;
	}

	public CollectorKey<?, ?>[] collectorKeys() {
		return keys;
	}

	public CollectorManager<Collector, ?>[] managers() {
		return managers;
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		values = valuesSource.getValues( context );
		for ( int i = 0; i < collectors.length; i++ ) {
			for ( int j = 0; j < collectors[i].length; j++ ) {
				leafCollectors[i][j] = collectors[i][j].getLeafCollector( context );
			}
		}
	}

	@Override
	public void finish() throws IOException {
		values = null;
	}

}
