/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.facet.impl;

import java.io.IOException;
import java.util.List;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollector.MatchingDocs;
import org.apache.lucene.facet.range.DoubleRange;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.NumericUtils;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.NumericDoubleValues;

/**
 * <p>
 * Copied with some changes from {@code org.apache.lucene.facet.range.DoubleRangeFacetCounts}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
public class DoubleMultiValueRangeFacetCounts extends MultiValueRangeFacetCounts {

	public DoubleMultiValueRangeFacetCounts(String field, DoubleMultiValuesToSingleValuesSource valueSource, FacetsCollector hits, DoubleRange... ranges) throws IOException {
		this( field, valueSource, hits, null, ranges );
	}

	public DoubleMultiValueRangeFacetCounts(String field, DoubleMultiValuesToSingleValuesSource valueSource, FacetsCollector hits, Query fastMatchQuery, DoubleRange... ranges) throws IOException {
		super( field, ranges, fastMatchQuery );
		count( valueSource, hits.getMatchingDocs() );
	}

	private void count(DoubleMultiValuesToSingleValuesSource valueSource, List<MatchingDocs> matchingDocs) throws IOException {

		DoubleRange[] doubleRanges = (DoubleRange[]) this.ranges;

		LongRange[] longRanges = new LongRange[doubleRanges.length];
		for ( int i = 0; i < doubleRanges.length; i++ ) {
			DoubleRange range = doubleRanges[i];
			longRanges[i] = new LongRange( range.label,
				NumericUtils.doubleToSortableLong( range.min ), true,
				NumericUtils.doubleToSortableLong( range.max ), true );
		}

		LongMultiValueRangeCounter counter = new LongMultiValueRangeCounter( longRanges );

		int missingCount = 0;
		for ( MatchingDocs hits : matchingDocs ) {
			NumericDoubleValues fv = valueSource.getValues( hits.context, null );

			final DocIdSetIterator fastMatchDocs;
			if ( fastMatchQuery != null ) {
				final IndexReaderContext topLevelContext = ReaderUtil.getTopLevelContext( hits.context );
				final IndexSearcher searcher = new IndexSearcher( topLevelContext );
				searcher.setQueryCache( null );
				final Weight fastMatchWeight = searcher.createWeight( searcher.rewrite( fastMatchQuery ), ScoreMode.COMPLETE_NO_SCORES, 1 );
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
					int count = fv.docValueCount();
					totCount += count;
					for ( int index = 0; index < count; ++index ) {
						counter.add( NumericUtils.doubleToSortableLong( fv.doubleValue() ) );
					}
				}
				else {
					missingCount++;
				}

				doc = docs.nextDoc();
			}
		}

		missingCount += counter.fillCounts( counts );
		totCount -= missingCount;
	}
}
