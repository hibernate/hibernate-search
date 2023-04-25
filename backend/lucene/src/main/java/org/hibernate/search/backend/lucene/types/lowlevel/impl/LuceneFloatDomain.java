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

import org.hibernate.search.backend.lucene.lowlevel.comparator.impl.FloatValuesSourceComparator;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.FacetCountsUtils;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.LongMultiValueFacetCounts;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.LongMultiValueRangeFacetCounts;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.NumericUtils;

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
	public Query createSetQuery(String absoluteFieldPath, Collection<Float> values) {
		return FloatPoint.newSetQuery( absoluteFieldPath, values );
	}

	@Override
	public Float sortedDocValueToTerm(long longValue) {
		return NumericUtils.sortableIntToFloat( (int) longValue );
	}

	@Override
	public Facets createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider)
			throws IOException {
		// As we don't need to apply any operation to terms except sometimes a sort,
		// we can simply rely on raw, int values, whose order is the same as their corresponding float value.
		// Values are ultimately converted back to the Float equivalent by calling sortedDocValueToTerm.
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromIntField(
				absoluteFieldPath, nestedDocsProvider
		);
		return new LongMultiValueFacetCounts( absoluteFieldPath, source, facetsCollector );
	}

	@Override
	public Facets createRangeFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			Collection<? extends Range<? extends Float>> ranges,
			NestedDocsProvider nestedDocsProvider)
			throws IOException {
		// As we don't need to apply any operation to terms except sometimes a sort,
		// we can simply rely on raw, int values, whose order is the same as their corresponding float value.
		// Values are ultimately converted back to the Float equivalent by calling sortedDocValueToTerm.
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromIntField(
				absoluteFieldPath, nestedDocsProvider
		);
		return new LongMultiValueRangeFacetCounts(
				absoluteFieldPath, source,
				facetsCollector,
				FacetCountsUtils.createLongRangesForFloatingPointValues(
						ranges, value -> (long) NumericUtils.floatToSortableInt( value ),
						Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY
				)
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
	public FieldComparator<Float> createFieldComparator(String fieldName, int numHits,
			Float missingValue, boolean reversed, boolean enableSkipping, MultiValueMode multiValueMode,
			NestedDocsProvider nestedDocsProvider) {
		DoubleMultiValuesToSingleValuesSource source =
				DoubleMultiValuesToSingleValuesSource.fromFloatField( fieldName, multiValueMode, nestedDocsProvider );
		return new FloatValuesSourceComparator( numHits, fieldName, missingValue, reversed, enableSkipping, source );
	}

}
