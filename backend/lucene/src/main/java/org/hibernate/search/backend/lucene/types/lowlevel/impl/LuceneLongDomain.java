/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.facet.impl.FacetCountsUtils;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LongValueFacetCounts;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;

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
	public Long fromDocValue(Long longValue) {
		return longValue;
	}

	@Override
	public LongValueFacetCounts createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		// TODO HSEARCH-3856 aggregations on multi-valued fields - currently we just use the minimum value
		LongMultiValuesToSingleValuesSource source = LongMultiValuesToSingleValuesSource.fromLongField(
				absoluteFieldPath, MultiValueMode.MIN, nestedDocsProvider
		);
		return new LongValueFacetCounts(
			absoluteFieldPath, source,
			facetsCollector
		);
	}

	@Override
	public Facets createRangeFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			Collection<? extends Range<? extends Long>> ranges,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		// TODO HSEARCH-3856 aggregations on multi-valued fields - currently we just use the minimum value
		LongMultiValuesToSingleValuesSource source = LongMultiValuesToSingleValuesSource.fromLongField(
				absoluteFieldPath, MultiValueMode.MIN, nestedDocsProvider
		);
		return new LongRangeFacetCounts(
			absoluteFieldPath, source,
			facetsCollector, FacetCountsUtils.createLongRanges( ranges )
		);
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Long numericValue) {
		return new LongPoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createDocValuesField(String absoluteFieldPath, Long numericValue) {
		return new NumericDocValuesField( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createSortedDocValuesField(String absoluteFieldPath, Long numericValue) {
		return new SortedNumericDocValuesField( absoluteFieldPath, numericValue );
	}

	@Override
	public FieldComparator.NumericComparator<Long> createFieldComparator(String fieldName, int numHits,
			MultiValueMode multiValueMode, Long missingValue, NestedDocsProvider nestedDocsProvider) {
		LongMultiValuesToSingleValuesSource source = LongMultiValuesToSingleValuesSource.fromLongField( fieldName, multiValueMode, nestedDocsProvider );
		return new LongFieldComparator( numHits, fieldName, missingValue, source );
	}

	public static class LongFieldComparator extends FieldComparator.LongComparator {

		private final LongMultiValuesToSingleValuesSource source;

		public LongFieldComparator(int numHits, String field, Long missingValue, LongMultiValuesToSingleValuesSource source) {
			super( numHits, field, missingValue );
			this.source = source;
		}

		@Override
		protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
			return source.getRawNumericDocValues( context, null );
		}
	}
}
