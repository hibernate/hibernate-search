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
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

public class LongMultiValueRangeFacetCounts extends MultiValueRangeFacetCounts {

	public LongMultiValueRangeFacetCounts(String field, MultiValueMode mode, NestedDocsProvider nested, FacetsCollector hits, LongRange... ranges) throws IOException {
		this( field, LongMultiValuesSource.fromLongField( field, mode, nested ), hits, ranges );
	}

	public LongMultiValueRangeFacetCounts(String field, LongMultiValuesSource valueSource, FacetsCollector hits, LongRange... ranges) throws IOException {
		this( field, valueSource, hits, null, ranges );
	}

	public LongMultiValueRangeFacetCounts(String field, LongMultiValuesSource valueSource, FacetsCollector hits, Query fastMatchQuery, LongRange... ranges) throws IOException {
		super( field, ranges, fastMatchQuery );
		count( valueSource, hits.getMatchingDocs() );
	}

	private void count(LongMultiValuesSource valueSource, List<FacetsCollector.MatchingDocs> matchingDocs) throws IOException {

		LongRange[] longRanges = (LongRange[]) this.ranges;

		LongMultiValueRangeCounter counter = new LongMultiValueRangeCounter( longRanges );

		int missingCount = 0;
		for ( FacetsCollector.MatchingDocs hits : matchingDocs ) {
			LongMultiValues fv = valueSource.getValues( hits.context, null );

			totCount += hits.totalHits;
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
					for ( int index = 0; index < count; ++index ) {
						counter.add( fv.nextValue() );
					}
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
