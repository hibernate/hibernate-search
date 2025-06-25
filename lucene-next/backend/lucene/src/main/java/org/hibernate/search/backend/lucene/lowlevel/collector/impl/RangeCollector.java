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
import org.hibernate.search.backend.lucene.types.aggregation.impl.EffectiveRange;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.cursors.IntCursor;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class RangeCollector extends SimpleCollector {

	private final LongMultiValuesSource valuesSource;
	private final LongRangeNode root;
	private final long[] boundaries;
	private final long[] countsPerBoundaries;

	private int leafUpto;
	private boolean filled = false;
	private final long[] counts;

	private LongMultiValues values;

	public RangeCollector(LongMultiValuesSource valuesSource, EffectiveRange[] ranges) {
		this.valuesSource = valuesSource;

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
		List<InclusiveRange> elementaryIntervals = new ArrayList<>();
		int upto0 = 1;
		long v = endsList.get( 0 );
		long prev;
		if ( endsMap.get( v ) == 3 ) {
			elementaryIntervals.add( new InclusiveRange( v, v ) );
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
					elementaryIntervals.add( new InclusiveRange( prev, v - 1 ) );
				}
				elementaryIntervals.add( new InclusiveRange( v, v ) );
				prev = v + 1;
			}
			else if ( flags == 1 ) {
				// This point is only the start of an interval;
				// attach it to next interval:
				if ( v > prev ) {
					elementaryIntervals.add( new InclusiveRange( prev, v - 1 ) );
				}
				prev = v;
			}
			else {
				assert flags == 2;
				// This point is only the end of an interval; attach
				// it to last interval:
				elementaryIntervals.add( new InclusiveRange( prev, v ) );
				prev = v + 1;
			}
			upto0++;
		}

		// Build binary tree on top of intervals:
		root = split( 0, elementaryIntervals.size(), elementaryIntervals );

		// Set outputs, so we know which range to output for
		// each node in the tree:
		for ( int i = 0; i < ranges.length; i++ ) {
			root.addOutputs( i, ranges[i] );
		}

		// Set boundaries (ends of each elementary interval):
		boundaries = new long[elementaryIntervals.size()];
		for ( int i = 0; i < boundaries.length; i++ ) {
			boundaries[i] = elementaryIntervals.get( i ).end;
		}

		countsPerBoundaries = new long[boundaries.length];
		counts = new long[ranges.length];
	}

	private void incrementCountForLeafWithIndex(int index) {
		countsPerBoundaries[index]++;
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

	/** Fills counts corresponding to the original input
	 * ranges, returning the missing count (how many hits
	 * didn't match any ranges). */
	private void fillCounts(long[] counts) {
		leafUpto = 0;
		rollup( root, counts, false );
	}

	private long rollup(LongRangeNode node, long[] counts, boolean sawOutputs) {
		long count;
		sawOutputs |= node.outputs != null;
		if ( node.left != null ) {
			count = rollup( node.left, counts, sawOutputs );
			count += rollup( node.right, counts, sawOutputs );
		}
		else {
			// Leaf:
			count = countsPerBoundaries[leafUpto];
			leafUpto++;
		}
		if ( node.outputs != null ) {
			for ( IntCursor rangeIndexCursor : node.outputs ) {
				counts[rangeIndexCursor.value] += count;
			}
		}
		return count;
	}

	private static LongRangeNode split(int start, int end, List<InclusiveRange> elementaryIntervals) {
		if ( start == end - 1 ) {
			// leaf
			InclusiveRange range = elementaryIntervals.get( start );
			return new LongRangeNode( range.start, range.end, null, null );
		}
		else {
			int mid = ( start + end ) >>> 1;
			LongRangeNode left = split( start, mid, elementaryIntervals );
			LongRangeNode right = split( mid, end, elementaryIntervals );
			return new LongRangeNode( left.start, right.end, left, right );
		}
	}

	private record InclusiveRange(long start, long end) {
		private InclusiveRange {
			assert end >= start;
		}

		@Override
		public String toString() {
			return start + " to " + end;
		}
	}

	/** Holds one node of the segment tree. */
	private static class LongRangeNode {
		private final LongRangeNode left;
		private final LongRangeNode right;

		// Our range, inclusive:
		private final long start;
		private final long end;

		// Which range indices to output when a query goes
		// through this node:
		IntArrayList outputs;

		public LongRangeNode(long start, long end, LongRangeNode left, LongRangeNode right) {
			this.start = start;
			this.end = end;
			this.left = left;
			this.right = right;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			toString( sb, 0 );
			return sb.toString();
		}

		static void indent(StringBuilder sb, int depth) {
			for ( int i = 0; i < depth; i++ ) {
				sb.append( "  " );
			}
		}

		/** Recursively assigns range outputs to each node. */
		void addOutputs(int index, EffectiveRange range) {
			if ( start >= range.min() && end <= range.max() ) {
				// Our range is fully included in the incoming
				// range; add to our output list:
				if ( outputs == null ) {
					outputs = new IntArrayList();
				}
				outputs.add( index );
			}
			else if ( left != null ) {
				assert right != null;
				// Recurse:
				left.addOutputs( index, range );
				right.addOutputs( index, range );
			}
		}

		void toString(StringBuilder sb, int depth) {
			indent( sb, depth );
			if ( left == null ) {
				assert right == null;
				sb.append( "leaf: " ).append( start ).append( " to " ).append( end );
			}
			else {
				sb.append( "node: " ).append( start ).append( " to " ).append( end );
			}
			if ( outputs != null ) {
				sb.append( " outputs=" );
				sb.append( outputs );
			}
			sb.append( '\n' );

			if ( left != null ) {
				assert right != null;
				left.toString( sb, depth + 1 );
				right.toString( sb, depth + 1 );
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
					incrementCountForLeafWithIndex( leafIndex );
				}
			}
		}
	}

	public long[] counts() {
		if ( !filled ) {
			filled = true;
			fillCounts( counts );
		}
		return counts;
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		values = valuesSource.getValues( context );
	}

	public void finish() throws IOException {
		values = null;
	}

}
