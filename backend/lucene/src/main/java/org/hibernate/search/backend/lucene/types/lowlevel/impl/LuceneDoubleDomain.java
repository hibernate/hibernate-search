/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.io.IOException;
import java.util.Collection;
import org.apache.lucene.document.DoubleDocValuesField;

import org.hibernate.search.backend.lucene.lowlevel.facet.impl.FacetCountsUtils;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LongValueFacetCounts;
import org.apache.lucene.facet.range.DoubleRangeFacetCounts;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;

public class LuceneDoubleDomain implements LuceneNumericDomain<Double> {
	private static final LuceneNumericDomain<Double> INSTANCE = new LuceneDoubleDomain();

	public static LuceneNumericDomain<Double> get() {
		return INSTANCE;
	}

	@Override
	public Double getMinValue() {
		return -Double.MAX_VALUE;
	}

	@Override
	public Double getMaxValue() {
		return Double.MAX_VALUE;
	}

	@Override
	public Double getPreviousValue(Double value) {
		return Math.nextDown( value );
	}

	@Override
	public Double getNextValue(Double value) {
		return Math.nextUp( value );
	}

	@Override
	public Query createExactQuery(String absoluteFieldPath, Double value) {
		return DoublePoint.newExactQuery( absoluteFieldPath, value );
	}

	@Override
	public Query createRangeQuery(String absoluteFieldPath, Double lowerLimit, Double upperLimit) {
		return DoublePoint.newRangeQuery(
				absoluteFieldPath, lowerLimit, upperLimit
		);
	}

	@Override
	public Double fromDocValue(Long longValue) {
		// See createTermsFacetCounts: it's the reason we need this method
		// Using the reverse operation from Double.doubleToRawLongBits, which is used in DoubleDocValues.
		return Double.longBitsToDouble( longValue );
	}

	@Override
	public LongValueFacetCounts createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		// TODO HSEARCH-3856 aggregations on multi-valued fields - currently we just use the minimum value
		DoubleMultiValuesToSingleValuesSource source = DoubleMultiValuesToSingleValuesSource.fromDoubleField(
				absoluteFieldPath, MultiValueMode.MIN, nestedDocsProvider
		);
		return new LongValueFacetCounts(
				absoluteFieldPath,
				// We can't use DoubleValueSource here because it drops the decimals...
				// So we use this to get raw bits, and then apply fromDocValue to get back the original value.
				// must be getLongValuesSource().
				source.getLongValuesSource(),
				facetsCollector
		);
	}

	@Override
	public Facets createRangeFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			Collection<? extends Range<? extends Double>> ranges,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		// TODO HSEARCH-3856 aggregations on multi-valued fields - currently we just use the minimum value
		DoubleMultiValuesToSingleValuesSource source = DoubleMultiValuesToSingleValuesSource.fromDoubleField(
				absoluteFieldPath, MultiValueMode.MIN, nestedDocsProvider
		);
		return new DoubleRangeFacetCounts(
				absoluteFieldPath,
				source,
				facetsCollector, FacetCountsUtils.createDoubleRanges( ranges )
		);
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Double numericValue) {
		return new DoublePoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createDocValuesField(String absoluteFieldPath, Double numericValue) {
		return new DoubleDocValuesField( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createSortedDocValuesField(String absoluteFieldPath, Double numericValue) {
		return new SortedDoubleDocValuesField( absoluteFieldPath, numericValue );
	}

	@Override
	public FieldComparator.NumericComparator<Double> createFieldComparator(String fieldName, int numHits,
			MultiValueMode multiValueMode, Double missingValue, NestedDocsProvider nestedDocsProvider) {
		DoubleMultiValuesToSingleValuesSource source = DoubleMultiValuesToSingleValuesSource
				.fromDoubleField( fieldName, multiValueMode, nestedDocsProvider );

		return new DoubleFieldComparator( numHits, fieldName, missingValue, source );
	}

	public static class DoubleFieldComparator extends FieldComparator.DoubleComparator {

		private final DoubleMultiValuesToSingleValuesSource source;

		public DoubleFieldComparator(int numHits, String field, Double missingValue, DoubleMultiValuesToSingleValuesSource source) {
			super( numHits, field, missingValue );
			this.source = source;
		}

		@Override
		protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
			return source.getRawNumericDocValues( context, DoubleValues.withDefault( DoubleValues.EMPTY, missingValue ) );
		}
	}

	public static class SortedDoubleDocValuesField extends SortedNumericDocValuesField {

		public SortedDoubleDocValuesField(String name, double value) {
			super( name, Double.doubleToRawLongBits( value ) );
		}

		@Override
		public void setDoubleValue(double value) {
			super.setLongValue( Double.doubleToRawLongBits( value ) );
		}

		@Override
		public void setLongValue(long value) {
			throw new IllegalArgumentException( "cannot change value type from Double to Long" );
		}
	}

}
