/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.util.Collection;
import java.util.Comparator;

import org.hibernate.search.backend.lucene.lowlevel.comparator.impl.LongValuesSourceComparator;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.engine.cfg.spi.NumberUtils;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Pruning;
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
	public double sortedDocValueToDouble(long longValue) {
		return sortedDocValueToTerm( longValue ).doubleValue();
	}

	@Override
	public Long doubleToTerm(double doubleValue) {
		return NumberUtils.toLong( doubleValue );
	}

	@Override
	public EffectiveRange[] createEffectiveRanges(Collection<? extends Range<? extends Long>> ranges) {
		return EffectiveRange.createEffectiveRangesForIntegralValues( ranges );
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
			Long missingValue, boolean reversed, Pruning pruning, MultiValueMode multiValueMode,
			NestedDocsProvider nestedDocsProvider) {
		LongMultiValuesToSingleValuesSource source =
				LongMultiValuesToSingleValuesSource.fromLongField( fieldName, multiValueMode, nestedDocsProvider );
		return new LongValuesSourceComparator( numHits, fieldName, missingValue, reversed, pruning, source );
	}
}
