/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.util.Collection;
import java.util.Comparator;

import org.hibernate.search.backend.lucene.lowlevel.comparator.impl.FloatValuesSourceComparator;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.engine.cfg.spi.NumberUtils;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Pruning;
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
	public double sortedDocValueToDouble(long longValue) {
		return sortedDocValueToTerm( longValue ).doubleValue();
	}

	@Override
	public Float doubleToTerm(double doubleValue) {
		return NumberUtils.toFloat( doubleValue );
	}

	@Override
	public EffectiveRange[] createEffectiveRanges(Collection<? extends Range<? extends Float>> ranges) {
		return EffectiveRange.createEffectiveRangesForIntegralValues( ranges, NumericUtils::floatToSortableInt,
				Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY );
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
			Float missingValue, boolean reversed, Pruning pruning, MultiValueMode multiValueMode,
			NestedDocsProvider nestedDocsProvider) {
		DoubleMultiValuesToSingleValuesSource source =
				DoubleMultiValuesToSingleValuesSource.fromFloatField( fieldName, multiValueMode, nestedDocsProvider );
		return new FloatValuesSourceComparator( numHits, fieldName, missingValue, reversed, pruning, source );
	}

}
