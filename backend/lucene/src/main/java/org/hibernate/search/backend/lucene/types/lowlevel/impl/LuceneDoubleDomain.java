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

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningDoubleMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.FacetCountsUtils;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.NumericUtils;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.DoubleMultiValueRangeFacetCounts;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.LongMultiValueFacetCounts;

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
	public Comparator<Double> createComparator() {
		return Comparator.naturalOrder();
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
	public Double sortedDocValueToTerm(long longValue) {
		return NumericUtils.sortableLongToDouble( longValue );
	}

	@Override
	public Facets createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		// As we don't need to apply any operation to terms except sometimes a sort,
		// we can simply rely on raw, long values, whose order is the same as their corresponding double value.
		// Values are ultimately converted back to the Double equivalent by calling sortedDocValueToTerm.
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromLongField(
				absoluteFieldPath, nestedDocsProvider
		);
		return new LongMultiValueFacetCounts( absoluteFieldPath, source, facetsCollector );
	}

	@Override
	public Facets createRangeFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			Collection<? extends Range<? extends Double>> ranges,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		JoiningDoubleMultiValuesSource source = JoiningDoubleMultiValuesSource.fromDoubleField(
				absoluteFieldPath, nestedDocsProvider
		);
		return new DoubleMultiValueRangeFacetCounts(
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
	public IndexableField createSortedDocValuesField(String absoluteFieldPath, Double numericValue) {
		return new SortedNumericDocValuesField( absoluteFieldPath, NumericUtils.doubleToSortableLong( numericValue ) );
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
			return source.getValues( context, DoubleValues.withDefault( DoubleValues.EMPTY, missingValue ) ).getRawDoubleValues();
		}
	}

}
