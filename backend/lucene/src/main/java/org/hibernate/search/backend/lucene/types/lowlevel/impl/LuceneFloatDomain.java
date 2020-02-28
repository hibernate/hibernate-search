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

import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LongValueFacetCounts;
import org.apache.lucene.facet.range.DoubleRangeFacetCounts;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BitSet;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.FieldData;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.SortedNumericDoubleValues;

public class LuceneFloatDomain implements LuceneNumericDomain<Float> {
	private static final LuceneNumericDomain<Float> INSTANCE = new LuceneFloatDomain();

	public static LuceneNumericDomain<Float> get() {
		return INSTANCE;
	}

	@Override
	public Float getMinValue() {
		return Float.MIN_VALUE;
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
	public Float fromDocValue(Long longValue) {
		// See createTermsFacetCounts: it's the reason we need this method
		// Using the reverse operation from Double.doubleToRawLongBits, which is used in DoubleDocValues.
		return Float.intBitsToFloat( longValue.intValue() );
	}

	@Override
	public LongValueFacetCounts createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector) throws IOException {
		return new LongValueFacetCounts(
			absoluteFieldPath,
			// We can't use DoubleValueSource here because it drops the decimals...
			// So we use this to get raw bits, and then apply fromDocValue to get back the original value.
			LongValuesSource.fromIntField( absoluteFieldPath ),
			facetsCollector
		);
	}

	@Override
	public Facets createRangeFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
		Collection<? extends Range<? extends Float>> ranges) throws IOException {
		return new DoubleRangeFacetCounts(
			absoluteFieldPath, DoubleValuesSource.fromFloatField( absoluteFieldPath ),
			facetsCollector, FacetCountsUtils.createDoubleRanges( ranges )
		);
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Float numericValue) {
		return new FloatPoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createDocValuesField(String absoluteFieldPath, Float numericValue) {
		return new FloatDocValuesField( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createSortedField(String absoluteFieldPath, Float numericValue) {
		return new SortedFloatDocValuesField( absoluteFieldPath, numericValue );
	}

	@Override
	public FieldComparator.NumericComparator<Float> createFieldComparator(String fieldname, int numHits, MultiValueMode sortMode, Float missingValue, NestedDocsProvider nestedDocsProvider) {
		return new FloatFieldComparator( numHits, sortMode, fieldname, missingValue, nestedDocsProvider );
	}

	public static class FloatFieldComparator extends FieldComparator.FloatComparator {

		private NestedDocsProvider nested;
		private final MultiValueMode sortMode;

		public FloatFieldComparator(int numHits, MultiValueMode sortMode, String field, Float missingValue, NestedDocsProvider nested) {
			super( numHits, field, missingValue );
			this.nested = nested;
			this.sortMode = sortMode;
		}

		@Override
		protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
			SortedNumericDocValues numericDocValues = DocValues.getSortedNumeric( context.reader(), field );
			final SortedNumericDoubleValues values = FieldData.castToFloat( numericDocValues );
			if ( nested == null ) {
				return FieldData.replaceMissing( sortMode.select( values ), missingValue ).getRawDoubleValues();
			}
			else {
				final BitSet rootDocs = nested.parentDocs( context );
				final DocIdSetIterator innerDocs = nested.childDocs( context );
				return sortMode.select( values, missingValue, rootDocs, innerDocs, context.reader().maxDoc(), Integer.MAX_VALUE ).getRawDoubleValues();
			}
		}
	}

	public static class SortedFloatDocValuesField extends SortedNumericDocValuesField {

		public SortedFloatDocValuesField(String name, float value) {
			super( name, Float.floatToRawIntBits( value ) );
		}

		@Override
		public void setFloatValue(float value) {
			super.setLongValue( Float.floatToRawIntBits( value ) );
		}

		@Override
		public void setLongValue(long value) {
			throw new IllegalArgumentException( "cannot change value type from Float to Long" );
		}
	}

}
