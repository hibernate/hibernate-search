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

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.procedures.IntProcedure;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollector.MatchingDocs;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.TopOrdAndIntQueue;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.MultiDocValues.MultiSortedSetDocValues;
import org.apache.lucene.index.OrdinalMap;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LongValues;

/**
 * Copied with some changes from {@code org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
public class TextMultiValueFacetCounts extends Facets {

	final SortedSetDocValues dv;
	final String field;
	final int ordCount;
	final int[] counts;

	public TextMultiValueFacetCounts(IndexReader reader, String field, TextMultiValuesSource valuesSource, FacetsCollector hits)
			throws IOException {
		this.field = field;
		dv = MultiDocValues.getSortedSetValues( reader, field );
		if ( dv != null && dv.getValueCount() > Integer.MAX_VALUE ) {
			// We may want to remove this limitation?
			// Note that DefaultSortedSetDocValuesReaderState has the same limitation,
			// so this is no worse than the "legacy" facets from Search 5.
			throw new IllegalStateException(
					"Cannot aggregate when more than " + Integer.MAX_VALUE + " terms are indexed" );
		}
		ordCount = dv == null ? 0 : (int) dv.getValueCount();
		counts = new int[ordCount];
		count( reader, valuesSource, hits.getMatchingDocs() );
	}

	@Override
	public FacetResult getAllChildren(String dim, String... path) {
		throw new UnsupportedOperationException(
				"Getting all children is not supported by " + this.getClass().getSimpleName() );
	}

	@Override
	public FacetResult getTopChildren(int topN, String dim, String... path) throws IOException {
		if ( topN <= 0 ) {
			throw new IllegalArgumentException( "topN must be > 0 (got: " + topN + ")" );
		}
		if ( !dim.equals( field ) ) {
			throw new IllegalArgumentException( "invalid dim \"" + dim + "\"; should be \"" + field + "\"" );
		}
		if ( path.length != 0 ) {
			throw new IllegalArgumentException( "path.length should be 0" );
		}
		return getTopChildrenSortByCount( topN );
	}

	private FacetResult getTopChildrenSortByCount(int topN) throws IOException {
		if ( topN > ordCount ) {
			// HSEARCH-4544 Avoid OutOfMemoryError when passing crazy high topN values
			// We know there will never be more than "ordCount" values anyway.
			topN = ordCount;
		}

		TopOrdAndIntQueue q = null;

		int bottomCount = 0;

		int totCount = 0;
		int childCount = 0;

		TopOrdAndIntQueue.OrdAndValue reuse = null;

		for ( int ord = 0; ord < ordCount; ord++ ) {
			if ( counts[ord] > 0 ) {
				totCount += counts[ord];
				childCount++;
				if ( counts[ord] > bottomCount ) {
					if ( reuse == null ) {
						reuse = new TopOrdAndIntQueue.OrdAndValue();
					}
					reuse.ord = ord;
					reuse.value = counts[ord];
					if ( q == null ) {
						// Lazy init, so we don't create this for the
						// sparse case unnecessarily
						q = new TopOrdAndIntQueue( topN );
					}
					reuse = q.insertWithOverflow( reuse );
					if ( q.size() == topN ) {
						bottomCount = q.top().value;
					}
				}
			}
		}

		if ( q == null ) {
			return null;
		}

		LabelAndValue[] labelValues = new LabelAndValue[q.size()];
		for ( int i = labelValues.length - 1; i >= 0; i-- ) {
			TopOrdAndIntQueue.OrdAndValue ordAndValue = q.pop();
			final BytesRef term = dv.lookupOrd( ordAndValue.ord );
			labelValues[i] = new LabelAndValue( term.utf8ToString(), ordAndValue.value );
		}

		return new FacetResult( field, new String[0], totCount, labelValues, childCount );
	}

	private void countOneSegment(OrdinalMap ordinalMap, TextMultiValues segValues, int segOrd, MatchingDocs hits)
			throws IOException {
		if ( segValues == null ) {
			// nothing to count
			return;
		}
		IntHashSet uniqueOrdinalsForDocument = new IntHashSet();

		DocIdSetIterator docs = hits.bits.iterator();

		// TODO: yet another option is to count all segs
		// first, only in seg-ord space, and then do a
		// merge-sort-PQ in the end to only "resolve to
		// global" those seg ords that can compete, if we know
		// we just want top K?  ie, this is the same algo
		// that'd be used for merging facets across shards
		// (distributed faceting).  but this has much higher
		// temp ram req'ts (sum of number of ords across all
		// segs)
		if ( ordinalMap != null ) {
			final LongValues ordMap = ordinalMap.getGlobalOrds( segOrd );

			int numSegOrds = (int) segValues.getValueCount();

			if ( hits.totalHits < numSegOrds / 10 ) {
				IntProcedure incrementCountForOrdinal = ord -> counts[ord]++;
				// Remap every ord to global ord as we iterate:
				for ( int doc = docs.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = docs.nextDoc() ) {
					if ( !segValues.advanceExact( doc ) ) {
						continue; // No value for this doc
					}
					while ( segValues.hasNextValue() ) {
						int term = (int) segValues.nextOrd();
						int globalOrd = (int) ordMap.get( term );
						uniqueOrdinalsForDocument.add( globalOrd );
					}
					uniqueOrdinalsForDocument.forEach( incrementCountForOrdinal );
					uniqueOrdinalsForDocument.clear();
				}
			}
			else {
				// First count in seg-ord space:
				final int[] segCounts = new int[numSegOrds];
				IntProcedure incrementCountForOrdinal = ord -> segCounts[ord]++;
				for ( int doc = docs.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = docs.nextDoc() ) {
					if ( !segValues.advanceExact( doc ) ) {
						continue; // No value for this doc
					}
					while ( segValues.hasNextValue() ) {
						int term = (int) segValues.nextOrd();
						uniqueOrdinalsForDocument.add( term );
					}
					uniqueOrdinalsForDocument.forEach( incrementCountForOrdinal );
					uniqueOrdinalsForDocument.clear();
				}

				// Then, migrate to global ords:
				for ( int ord = 0; ord < numSegOrds; ord++ ) {
					int count = segCounts[ord];
					if ( count != 0 ) {
						counts[(int) ordMap.get( ord )] += count;
					}
				}
			}
		}
		else {
			// No ord mapping (e.g., single segment index):
			// just aggregate directly into counts.
			IntProcedure incrementCountForOrdinal = ord -> counts[ord]++;
			for ( int doc = docs.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = docs.nextDoc() ) {
				if ( !segValues.advanceExact( doc ) ) {
					continue; // No value for this doc
				}
				while ( segValues.hasNextValue() ) {
					int term = (int) segValues.nextOrd();
					uniqueOrdinalsForDocument.add( term );
				}
				uniqueOrdinalsForDocument.forEach( incrementCountForOrdinal );
				uniqueOrdinalsForDocument.clear();
			}
		}
	}

	/**
	 * Does all the "real work" of tallying up the counts.
	 */
	private void count(IndexReader reader, TextMultiValuesSource valuesSource, List<MatchingDocs> matchingDocs)
			throws IOException {
		OrdinalMap ordinalMap;

		// TODO: is this right?  really, we need a way to
		// verify that this ordinalMap "matches" the leaves in
		// matchingDocs...
		if ( dv instanceof MultiSortedSetDocValues && matchingDocs.size() > 1 ) {
			ordinalMap = ( (MultiSortedSetDocValues) dv ).mapping;
		}
		else {
			ordinalMap = null;
		}

		for ( MatchingDocs hits : matchingDocs ) {

			// LUCENE-5090: make sure the provided reader context "matches"
			// the top-level reader passed to the
			// SortedSetDocValuesReaderState, else cryptic
			// AIOOBE can happen:
			if ( ReaderUtil.getTopLevelContext( hits.context ).reader() != reader ) {
				throw new IllegalStateException(
						"the SortedSetDocValuesReaderState provided to this class does not match the reader being searched; you must create a new SortedSetDocValuesReaderState every time you open a new IndexReader" );
			}

			countOneSegment( ordinalMap, valuesSource.getValues( hits.context ), hits.context.ord, hits );
		}
	}

	@Override
	public Number getSpecificValue(String dim, String... path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FacetResult> getAllDims(int topN) throws IOException {
		return Collections.singletonList( getTopChildren( topN, field ) );
	}

}
