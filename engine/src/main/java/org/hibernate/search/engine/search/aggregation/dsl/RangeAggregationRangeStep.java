/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Collection;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.data.Range;

/**
 * The step in a "range" aggregation definition where the ranges can be set.
 *
 * @param <N> The type of the next step.
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 * @param <F> The type of the targeted field.
 */
public interface RangeAggregationRangeStep<
		N extends RangeAggregationRangeMoreStep<?, ?, PDF, F>,
		PDF extends SearchPredicateFactory,
		F> {

	/**
	 * Add a bucket for the range {@code [lowerBound, upperBound)} (lower bound included, upper bound excluded),
	 * or {@code (lowerBound, upperBound)} (both bounds excluded) if the lower bound is {@code -Infinity}.
	 *
	 * @param lowerBound The lower bound of the range.
	 * @param upperBound The upper bound of the range.
	 * @return The next step.
	 */
	default N range(F lowerBound, F upperBound) {
		return range( Range.canonical( lowerBound, upperBound ) );
	}

	/**
	 * Add a bucket for given range.
	 *
	 * @param range The range to add.
	 * @return The next step.
	 *
	 * @see Range
	 */
	N range(Range<? extends F> range);

	/**
	 * Add one bucket for each of the given ranges.
	 *
	 * @param ranges The ranges to add.
	 * @return The next step.
	 *
	 * @see Range
	 */
	N ranges(Collection<? extends Range<? extends F>> ranges);

}
