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

import org.hibernate.search.backend.lucene.lowlevel.comparator.impl.LongValuesSourceComparator;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.FacetCountsUtils;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.LongMultiValueFacetCounts;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.LongMultiValueRangeFacetCounts;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;

public class LuceneLongDomain implements LuceneNumericDomain<Long> {
	private static final LuceneNumericDomain<Long> INSTANCE = new LuceneLongDomain();

	public static LuceneNumericDomain<Long> get() {
		return INSTANCE;
	}

	@Override
	public Long getMinValue() {
		return Long.MIN_VALUE;
	}

	@Override
	public Long getMaxValue() {
		return Long.MAX_VALUE;
	}

	@Override
	public Long getPreviousValue(Long value) {
		return Math.addExact( value, -1L );
	}

	@Override
	public Long getNextValue(Long value) {
		return Math.addExact( value, 1L );
	}

	@Override
	public Comparator<Long> createComparator() {
		return Comparator.naturalOrder();
	}

	@Override
	public Query createExactQuery(String absoluteFieldPath, Long value) {
		return LongPoint.newExactQuery( absoluteFieldPath, value );
	}

	@Override
	public Query createRangeQuery(String absoluteFieldPath, Long lowerLimit, Long upperLimit) {
		return LongPoint.newRangeQuery(
				absoluteFieldPath, lowerLimit, upperLimit
		);
	}

	@Override
	public Query createSetQuery(String absoluteFieldPath, Collection<Long> values) {
		return LongPoint.newSetQuery( absoluteFieldPath, values );
	}

	@Override
	public Long sortedDocValueToTerm(long longValue) {
		return longValue;
	}

	@Override
	public Facets createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider)
			throws IOException {
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromLongField(
				absoluteFieldPath, nestedDocsProvider
		);
		return new LongMultiValueFacetCounts(
				absoluteFieldPath, source,
				facetsCollector
		);
	}

	@Override
	public Facets createRangeFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			Collection<? extends Range<? extends Long>> ranges,
			NestedDocsProvider nestedDocsProvider)
			throws IOException {
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromLongField(
				absoluteFieldPath, nestedDocsProvider
		);
		return new LongMultiValueRangeFacetCounts(
				absoluteFieldPath, source,
				facetsCollector,
				FacetCountsUtils.createLongRangesForIntegralValues( ranges )
		);
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Long numericValue) {
		return new LongPoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createSortedDocValuesField(String absoluteFieldPath, Long numericValue) {
		return new SortedNumericDocValuesField( absoluteFieldPath, numericValue );
	}

	@Override
	public FieldComparator<Long> createFieldComparator(String fieldName, int numHits,
			Long missingValue, boolean reversed, boolean enableSkipping, MultiValueMode multiValueMode,
			NestedDocsProvider nestedDocsProvider) {
		LongMultiValuesToSingleValuesSource source =
				LongMultiValuesToSingleValuesSource.fromLongField( fieldName, multiValueMode, nestedDocsProvider );
		return new LongValuesSourceComparator( numHits, fieldName, missingValue, reversed, enableSkipping, source );
	}
}
