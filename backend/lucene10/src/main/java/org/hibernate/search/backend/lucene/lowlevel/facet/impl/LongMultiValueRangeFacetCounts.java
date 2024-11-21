/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.facet.impl;

import java.io.IOException;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.procedures.IntProcedure;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

/**
 * <p>
 * Copied with some changes from {@code org.apache.lucene.facet.range.LongRangeFacetCounts}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
public class LongMultiValueRangeFacetCounts extends MultiValueRangeFacetCounts {

	public LongMultiValueRangeFacetCounts(String field, LongMultiValuesSource valueSource, FacetsCollector hits,
			LongRange... ranges)
			throws IOException {
		this( field, valueSource, hits, null, ranges );
	}

	public LongMultiValueRangeFacetCounts(String field, LongMultiValuesSource valueSource, FacetsCollector hits,
			Query fastMatchQuery, LongRange... ranges)
			throws IOException {
		super( field, ranges, fastMatchQuery );
		count( valueSource, hits.getMatchingDocs() );
	}

	private void count(LongMultiValuesSource valueSource, List<FacetsCollector.MatchingDocs> matchingDocs) throws IOException {
		LongRange[] longRanges = (LongRange[]) this.ranges;

		IntHashSet uniqueLeafIndicesForDocument = new IntHashSet();
		LongMultiValueRangeCounter counter = new LongMultiValueRangeCounter( longRanges );
		IntProcedure incrementCountForLeafWithIndex = counter::incrementCountForLeafWithIndex;

		int missingCount = 0;
		for ( FacetsCollector.MatchingDocs hits : matchingDocs ) {
			LongMultiValues fv = valueSource.getValues( hits.context );

			totCount += hits.totalHits;
			final DocIdSetIterator fastMatchDocs;
			if ( fastMatchQuery != null ) {
				final IndexReaderContext topLevelContext = ReaderUtil.getTopLevelContext( hits.context );
				final IndexSearcher searcher = new IndexSearcher( topLevelContext );
				searcher.setQueryCache( null );
				final Weight fastMatchWeight =
						searcher.createWeight( searcher.rewrite( fastMatchQuery ), ScoreMode.COMPLETE_NO_SCORES, 1 );
				Scorer s = fastMatchWeight.scorer( hits.context );
				if ( s == null ) {
					continue;
				}
				fastMatchDocs = s.iterator();
			}
			else {
				fastMatchDocs = null;
			}

			DocIdSetIterator docs = hits.bits.iterator();
			for ( int doc = docs.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; ) {
				if ( fastMatchDocs != null ) {
					int fastMatchDoc = fastMatchDocs.docID();
					if ( fastMatchDoc < doc ) {
						fastMatchDoc = fastMatchDocs.advance( doc );
					}

					if ( doc != fastMatchDoc ) {
						doc = docs.advance( fastMatchDoc );
						continue;
					}
				}

				if ( fv.advanceExact( doc ) ) {
					while ( fv.hasNextValue() ) {
						// Each document must be counted only once per range.
						int leafIndex = counter.findLeafIndex( fv.nextValue() );
						uniqueLeafIndicesForDocument.add( leafIndex );
					}

					uniqueLeafIndicesForDocument.forEach( incrementCountForLeafWithIndex );
					uniqueLeafIndicesForDocument.clear();
				}
				else {
					missingCount++;
				}

				doc = docs.nextDoc();
			}
		}

		int x = counter.fillCounts( counts );

		missingCount += x;

		totCount -= missingCount;
	}
}
