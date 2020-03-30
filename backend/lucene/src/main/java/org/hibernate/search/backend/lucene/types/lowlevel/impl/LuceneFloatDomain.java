/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.NumericDoubleValues;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.FacetCountsUtils;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LongValueFacetCounts;
import org.apache.lucene.facet.range.DoubleRangeFacetCounts;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.NumericUtils;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;

public class LuceneFloatDomain implements LuceneNumericDomain<Float> {
	private static final LuceneNumericDomain<Float> INSTANCE = new LuceneFloatDomain();

	public static LuceneNumericDomain<Float> get() {
		return INSTANCE;
	}

	@Override
	public Float getMinValue() {
		return -Float.MAX_VALUE;
	}

	@Override
	public Float getMaxValue() {
		return Float.MAX_VALUE;
	}

	@Override
	public Float getPreviousValue(Float value) {
		return Math.nextDown( value );
	}

	@Override
	public Float getNextValue(Float value) {
		return Math.nextUp( value );
	}

	@Override
	public Comparator<Float> createComparator() {
		return Comparator.naturalOrder();
	}

	@Override
	public Query createExactQuery(String absoluteFieldPath, Float value) {
		return FloatPoint.newExactQuery( absoluteFieldPath, value );
	}

	@Override
	public Query createRangeQuery(String absoluteFieldPath, Float lowerLimit, Float upperLimit) {
		return FloatPoint.newRangeQuery(
				absoluteFieldPath, lowerLimit, upperLimit
		);
	}

	@Override
	public Float rawFacetTermToTerm(long longValue) {
		// See createTermsFacetCounts: it's the reason we need this method
		// Using the reverse operation from Double.doubleToRawLongBits, which is used in DoubleDocValues.
		return Float.intBitsToFloat( (int) longValue );
	}

	@Override
	public Float sortedDocValueToTerm(long longValue) {
		return NumericUtils.sortableIntToFloat( (int) longValue );
	}

	@Override
	public LongValueFacetCounts createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		// TODO HSEARCH-3856 aggregations on multi-valued fields - currently we just use the minimum value
		DoubleMultiValuesToSingleValuesSource source = DoubleMultiValuesToSingleValuesSource.fromFloatField(
				absoluteFieldPath, MultiValueMode.MIN, nestedDocsProvider
		);
		return new LongValueFacetCounts(
				absoluteFieldPath,
				// We can't use DoubleValueSource.toLongValuesSource() here because it drops the decimals...
				// So we use this to get raw bits, and then apply fromDocValue to get back the original value.
				source.toRawValuesSource( NumericDoubleValues::getRawFloatValues ),
				facetsCollector
		);
	}

	@Override
	public Facets createRangeFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			Collection<? extends Range<? extends Float>> ranges,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		// TODO HSEARCH-3856 aggregations on multi-valued fields - currently we just use the minimum value
		DoubleMultiValuesToSingleValuesSource source = DoubleMultiValuesToSingleValuesSource.fromFloatField(
				absoluteFieldPath, MultiValueMode.MIN, nestedDocsProvider
		);
		return new DoubleRangeFacetCounts(
				absoluteFieldPath, source,
				facetsCollector, FacetCountsUtils.createDoubleRanges( ranges )
		);
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Float numericValue) {
		return new FloatPoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createSortedDocValuesField(String absoluteFieldPath, Float numericValue) {
		return new SortedNumericDocValuesField( absoluteFieldPath, NumericUtils.floatToSortableInt( numericValue ) );
	}

	@Override
	public FieldComparator.NumericComparator<Float> createFieldComparator(String fieldname, int numHits,
			MultiValueMode multiValueMode, Float missingValue, NestedDocsProvider nestedDocsProvider) {
		DoubleMultiValuesToSingleValuesSource source = DoubleMultiValuesToSingleValuesSource.fromFloatField( fieldname, multiValueMode, nestedDocsProvider );
		return new FloatFieldComparator( numHits, fieldname, missingValue, source );
	}

	public static class FloatFieldComparator extends FieldComparator.FloatComparator {
		private final DoubleMultiValuesToSingleValuesSource source;

		public FloatFieldComparator(int numHits, String field, Float missingValue, DoubleMultiValuesToSingleValuesSource source) {
			super( numHits, field, missingValue );
			this.source = source;
		}

		@Override
		protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
			return source.getValues( context, null ).getRawFloatValues();
		}
	}

}
