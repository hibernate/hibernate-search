/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LongValueFacetCounts;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BitSet;

public class LuceneIntegerDomain implements LuceneNumericDomain<Integer> {
	private static final LuceneNumericDomain<Integer> INSTANCE = new LuceneIntegerDomain();

	public static LuceneNumericDomain<Integer> get() {
		return INSTANCE;
	}

	@Override
	public Integer getMinValue() {
		return Integer.MIN_VALUE;
	}

	@Override
	public Integer getMaxValue() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Integer getPreviousValue(Integer value) {
		return Math.addExact( value, -1 );
	}

	@Override
	public Integer getNextValue(Integer value) {
		return Math.addExact( value, 1 );
	}

	@Override
	public Query createExactQuery(String absoluteFieldPath, Integer value) {
		return IntPoint.newExactQuery( absoluteFieldPath, value );
	}

	@Override
	public Query createRangeQuery(String absoluteFieldPath, Integer lowerLimit, Integer upperLimit) {
		return IntPoint.newRangeQuery(
				absoluteFieldPath, lowerLimit, upperLimit
		);
	}

	@Override
	public Integer fromDocValue(Long longValue) {
		return longValue.intValue();
	}

	@Override
	public LongValueFacetCounts createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector) throws IOException {
		return new LongValueFacetCounts(
				absoluteFieldPath, LongValuesSource.fromIntField( absoluteFieldPath ),
				facetsCollector
		);
	}

	@Override
	public Facets createRangeFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			Collection<? extends Range<? extends Integer>> ranges) throws IOException {
		return new LongRangeFacetCounts(
				absoluteFieldPath, LongValuesSource.fromIntField( absoluteFieldPath ),
				facetsCollector, FacetCountsUtils.createLongRanges( ranges )
		);
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Integer numericValue) {
		return new IntPoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createDocValuesField(String absoluteFieldPath, Integer numericValue) {
		return new NumericDocValuesField( absoluteFieldPath, numericValue.longValue() );
	}

	@Override
	public FieldComparator.NumericComparator<Integer> createFieldComparator(String fieldName, int numHits, Integer missingValue, NestedDocsProvider nestedDocsProvider) {
		return new IntegerFieldComparator( numHits, fieldName, missingValue, nestedDocsProvider );
	}

	public static class IntegerFieldComparator extends FieldComparator.IntComparator {
		private NestedDocsProvider nestedDocsProvider;

		public IntegerFieldComparator(int numHits, String field, Integer missingValue, NestedDocsProvider nestedDocsProvider) {
			super( numHits, field, missingValue );
			this.nestedDocsProvider = nestedDocsProvider;
		}

		@Override
		protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
			NumericDocValues numericDocValues = super.getNumericDocValues( context, field );
			if ( nestedDocsProvider == null ) {
				return numericDocValues;
			}

			SortedNumericDocValues sortedNumericDocValues = DocValues.singleton( numericDocValues );
			BitSet parentDocs = nestedDocsProvider.parentDocs( context );
			DocIdSetIterator childDocs = nestedDocsProvider.childDocs( context );
			if ( parentDocs != null && childDocs != null ) {
				numericDocValues = OnTheFlyNestedSorter.sort( sortedNumericDocValues, missingValue, parentDocs, childDocs );
			}

			return numericDocValues;
		}
	}
}
