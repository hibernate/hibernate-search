/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.util.Collection;
import java.util.Comparator;

import org.hibernate.search.backend.lucene.lowlevel.comparator.impl.DoubleValuesSourceComparator;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Pruning;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.NumericUtils;

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
	public Query createSetQuery(String absoluteFieldPath, Collection<Double> values) {
		return DoublePoint.newSetQuery( absoluteFieldPath, values );
	}

	@Override
	public Double sortedDocValueToTerm(long longValue) {
		return NumericUtils.sortableLongToDouble( longValue );
	}

	@Override
	public double sortedDocValueToDouble(long longValue) {
		return sortedDocValueToTerm( longValue );
	}

	@Override
	public Double doubleToTerm(double doubleValue) {
		return doubleValue;
	}

	@Override
	public EffectiveRange[] createEffectiveRanges(Collection<? extends Range<? extends Double>> ranges) {
		return EffectiveRange.createEffectiveRangesForIntegralValues( ranges, NumericUtils::doubleToSortableLong,
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY );
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
	public FieldComparator<Double> createFieldComparator(String fieldName, int numHits,
			Double missingValue, boolean reversed, Pruning pruning, MultiValueMode multiValueMode,
			NestedDocsProvider nestedDocsProvider) {
		DoubleMultiValuesToSingleValuesSource source = DoubleMultiValuesToSingleValuesSource
				.fromDoubleField( fieldName, multiValueMode, nestedDocsProvider );
		return new DoubleValuesSourceComparator( numHits, fieldName, missingValue, reversed, pruning, source );
	}

}
