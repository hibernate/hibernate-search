/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.facet.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.cursors.LongIntCursor;
import com.carrotsearch.hppc.procedures.LongProcedure;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.PriorityQueue;

/**
 * <p>
 * Copied with some changes from {@code org.apache.lucene.facet.LongValueFacetCounts}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
public class LongMultiValueFacetCounts extends Facets {

	private final int[] counts = new int[1024];

	private final LongIntMap hashCounts = new LongIntHashMap();

	private final String field;

	private int totCount;

	public LongMultiValueFacetCounts(String field, LongMultiValuesSource valueSource, FacetsCollector hits) throws IOException {
		this.field = field;
		count( valueSource, hits.getMatchingDocs() );
	}

	private void count(LongMultiValuesSource valueSource, List<FacetsCollector.MatchingDocs> matchingDocs) throws IOException {
		LongHashSet uniqueValuesForDocument = new LongHashSet();
		LongProcedure incrementCountForDocumentId = this::increment;

		for ( FacetsCollector.MatchingDocs hits : matchingDocs ) {
			LongMultiValues fv = valueSource.getValues( hits.context );

			DocIdSetIterator docs = hits.bits.iterator();
			for ( int doc = docs.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = docs.nextDoc() ) {
				if ( fv.advanceExact( doc ) ) {
					totCount++;
					while ( fv.hasNextValue() ) {
						// Each document must be counted only once per value.
						uniqueValuesForDocument.add( fv.nextValue() );
					}

					uniqueValuesForDocument.forEach( incrementCountForDocumentId );
					uniqueValuesForDocument.clear();
				}
			}
		}
	}

	private void increment(long value) {
		if ( value >= 0 && value < counts.length ) {
			counts[(int) value]++;
		}
		else {
			hashCounts.addTo( value, 1 );
		}
	}

	@Override
	public FacetResult getAllChildren(String dim, String... path) {
		throw new UnsupportedOperationException(
				"Getting all children is not supported by " + this.getClass().getSimpleName() );
	}

	@Override
	public FacetResult getTopChildren(int topN, String dim, String... path) {
		if ( !dim.equals( field ) ) {
			throw new IllegalArgumentException( "invalid dim \"" + dim + "\"; should be \"" + field + "\"" );
		}
		if ( path.length != 0 ) {
			throw new IllegalArgumentException( "path.length should be 0" );
		}
		return getTopChildrenSortByCount( topN );
	}

	private static class Entry {
		int count;
		long value;
	}

	public FacetResult getTopChildrenSortByCount(int topN) {
		PriorityQueue<Entry> pq = new PriorityQueue<Entry>( Math.min( topN, counts.length + hashCounts.size() ) ) {
			@Override
			protected boolean lessThan(Entry a, Entry b) {
				// sort by count descending, breaking ties by value ascending:
				return a.count < b.count || ( a.count == b.count && a.value > b.value );
			}
		};

		int childCount = 0;
		Entry e = null;
		for ( int i = 0; i < counts.length; i++ ) {
			if ( counts[i] != 0 ) {
				childCount++;
				if ( e == null ) {
					e = new Entry();
				}
				e.value = i;
				e.count = counts[i];
				e = pq.insertWithOverflow( e );
			}
		}

		if ( hashCounts.size() != 0 ) {
			childCount += hashCounts.size();
			for ( LongIntCursor c : hashCounts ) {
				int count = c.value;
				if ( count != 0 ) {
					e = insertEntry( pq, e, c, count );
				}
			}
		}

		LabelAndValue[] results = new LabelAndValue[pq.size()];
		while ( pq.size() != 0 ) {
			Entry entry = pq.pop();
			results[pq.size()] = new LabelAndValue( Long.toString( entry.value ), entry.count );
		}

		return new FacetResult( field, new String[0], totCount, results, childCount );
	}

	private Entry insertEntry(PriorityQueue<Entry> pq,
			Entry e, LongIntCursor c, int count) {
		if ( e == null ) {
			e = new Entry();
		}
		e.value = c.key;
		e.count = count;
		e = pq.insertWithOverflow( e );
		return e;
	}

	@Override
	public Number getSpecificValue(String dim, String... path) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FacetResult> getAllDims(int topN) throws IOException {
		return Collections.singletonList( getTopChildren( topN, field ) );
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append( "LongValueFacetCounts totCount=" );
		b.append( totCount );
		b.append( ":\n" );
		for ( int i = 0; i < counts.length; i++ ) {
			if ( counts[i] != 0 ) {
				b.append( "  " );
				b.append( i );
				b.append( " -> count=" );
				b.append( counts[i] );
				b.append( '\n' );
			}
		}

		if ( hashCounts.size() != 0 ) {
			for ( LongIntCursor c : hashCounts ) {
				if ( c.value != 0 ) {
					b.append( "  " );
					b.append( c.key );
					b.append( " -> count=" );
					b.append( c.value );
					b.append( '\n' );
				}
			}
		}

		return b.toString();
	}
}
