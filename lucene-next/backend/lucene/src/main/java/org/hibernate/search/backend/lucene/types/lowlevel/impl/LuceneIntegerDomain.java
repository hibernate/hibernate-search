/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.util.Collection;
import java.util.Comparator;

import org.hibernate.search.backend.lucene.lowlevel.comparator.impl.IntValuesSourceComparator;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.engine.cfg.spi.NumberUtils;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Pruning;
import org.apache.lucene.search.Query;

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
	public Comparator<Integer> createComparator() {
		return Comparator.naturalOrder();
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
	public Query createSetQuery(String absoluteFieldPath, Collection<Integer> values) {
		return IntPoint.newSetQuery( absoluteFieldPath, values );
	}

	@Override
	public Integer sortedDocValueToTerm(long longValue) {
		return (int) longValue;
	}

	@Override
	public Integer doubleToTerm(double doubleValue) {
		return NumberUtils.toInteger( doubleValue );
	}

	@Override
	public double sortedDocValueToDouble(long longValue) {
		return sortedDocValueToTerm( longValue ).doubleValue();
	}

	@Override
	public EffectiveRange[] createEffectiveRanges(Collection<? extends Range<? extends Integer>> ranges) {
		return EffectiveRange.createEffectiveRangesForIntegralValues( ranges );
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Integer numericValue) {
		return new IntPoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createSortedDocValuesField(String absoluteFieldPath, Integer numericValue) {
		return new SortedNumericDocValuesField( absoluteFieldPath, numericValue.longValue() );
	}

	@Override
	public FieldComparator<Integer> createFieldComparator(String fieldName, int numHits,
			Integer missingValue, boolean reversed, Pruning pruning, MultiValueMode multiValueMode,
			NestedDocsProvider nestedDocsProvider) {
		LongMultiValuesToSingleValuesSource source =
				LongMultiValuesToSingleValuesSource.fromIntField( fieldName, multiValueMode, nestedDocsProvider );
		return new IntValuesSourceComparator( numHits, fieldName, missingValue, reversed, pruning, source );
	}
}
