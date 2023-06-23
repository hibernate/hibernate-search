/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.facet.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.cursors.IntCursor;

import org.apache.lucene.facet.range.LongRange;

/**
 * <p>
 * Copied with some changes from {@code org.apache.lucene.facet.range.LongRangeCounter}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
class LongMultiValueRangeCounter {

	final LongRangeNode root;
	final long[] boundaries;
	final int[] leafCounts;

	// Used during rollup
	private int leafUpto;
	private int missingCount;

	public LongMultiValueRangeCounter(LongRange[] ranges) {
		// Maps all range inclusive endpoints to int flags; 1
		// = start of interval, 2 = end of interval.  We need to
		// track the start vs end case separately because if a
		// given point is both, then it must be its own
		// elementary interval:
		LongIntMap endsMap = new LongIntHashMap();

		endsMap.put( Long.MIN_VALUE, 1 );
		endsMap.put( Long.MAX_VALUE, 2 );

		for ( LongRange range : ranges ) {
			int cur = endsMap.get( range.min );
			if ( cur == 0 ) {
				endsMap.put( range.min, 1 );
			}
			else {
				endsMap.put( range.min, cur | 1 );
			}
			cur = endsMap.get( range.max );
			if ( cur == 0 ) {
				endsMap.put( range.max, 2 );
			}
			else {
				endsMap.put( range.max, cur | 2 );
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

		leafCounts = new int[boundaries.length];
	}

	public void incrementCountForLeafWithIndex(int index) {
		leafCounts[index]++;
	}

	public int findLeafIndex(long v) {
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
	public int fillCounts(int[] counts) {
		missingCount = 0;
		leafUpto = 0;
		rollup( root, counts, false );
		return missingCount;
	}

	private int rollup(LongRangeNode node, int[] counts, boolean sawOutputs) {
		int count;
		sawOutputs |= node.outputs != null;
		if ( node.left != null ) {
			count = rollup( node.left, counts, sawOutputs );
			count += rollup( node.right, counts, sawOutputs );
		}
		else {
			// Leaf:
			count = leafCounts[leafUpto];
			leafUpto++;
			if ( !sawOutputs ) {
				// This is a missing count (no output ranges were
				// seen "above" us):
				missingCount += count;
			}
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

	private static final class InclusiveRange {
		public final long start;
		public final long end;

		public InclusiveRange(long start, long end) {
			assert end >= start;
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			return start + " to " + end;
		}
	}

	/** Holds one node of the segment tree. */
	public static final class LongRangeNode {
		final LongRangeNode left;
		final LongRangeNode right;

		// Our range, inclusive:
		final long start;
		final long end;

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
		void addOutputs(int index, LongRange range) {
			if ( start >= range.min && end <= range.max ) {
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
}
