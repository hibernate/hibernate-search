/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.facet.impl;

import com.carrotsearch.hppc.LongIntScatterMap;
import com.carrotsearch.hppc.cursors.LongIntCursor;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.PriorityQueue;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.NumericLongValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesToSingleValuesSource;

/**
 * <p>
 * Copied with some changes from {@code org.apache.lucene.facet.LongValueFacetCounts}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
public class LongMultiValueFacetCounts extends Facets {

	private final int[] counts = new int[1024];

	private final LongIntScatterMap hashCounts = new LongIntScatterMap();

	private final String field;

	private int totCount;

	public LongMultiValueFacetCounts(String field, LongMultiValuesToSingleValuesSource valueSource, FacetsCollector hits) throws IOException {
		this.field = field;
		count( valueSource, hits.getMatchingDocs() );
	}

	public LongMultiValueFacetCounts(String field, LongMultiValuesToSingleValuesSource valueSource, IndexReader reader) throws IOException {
		this.field = field;
		countAll( valueSource, field, reader );
	}

	private void count(LongMultiValuesToSingleValuesSource valueSource, List<FacetsCollector.MatchingDocs> matchingDocs) throws IOException {

		for ( FacetsCollector.MatchingDocs hits : matchingDocs ) {
			NumericLongValues fv = valueSource.getValues( hits.context, null );

			DocIdSetIterator docs = hits.bits.iterator();
			for ( int doc = docs.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; ) {
				if ( fv.advanceExact( doc ) ) {
					int count = fv.docValueCount();
					for ( int index = 0; index < count; ++index ) {
						increment( fv.longValue() );
						totCount++;
					}
				}

				doc = docs.nextDoc();
			}
		}
	}

	private void countAll(LongMultiValuesToSingleValuesSource valueSource, String field, IndexReader reader) throws IOException {

		for ( LeafReaderContext context : reader.leaves() ) {
			NumericLongValues fv = valueSource.getValues( context, null );
			int maxDoc = context.reader().maxDoc();

			for ( int doc = 0; doc < maxDoc; doc++ ) {
				if ( fv.advanceExact( doc ) ) {
					int count = fv.docValueCount();
					for ( int index = 0; index < count; ++index ) {
						increment( fv.longValue() );
						totCount++;
					}
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
	public FacetResult getTopChildren(int topN, String dim, String... path) {
		if ( dim.equals( field ) == false ) {
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
				return a.count < b.count || (a.count == b.count && a.value > b.value);
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
					if ( e == null ) {
						e = new Entry();
					}
					e.value = c.key;
					e.count = count;
					e = pq.insertWithOverflow( e );
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
