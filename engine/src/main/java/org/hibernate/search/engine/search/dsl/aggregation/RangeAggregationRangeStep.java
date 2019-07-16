/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.aggregation;

import java.util.Collection;

import org.hibernate.search.util.common.data.Range;

/**
 * The step in a "range" aggregation definition where the ranges can be set.
 *
 * @param <F> The type of the targeted field.
 */
public interface RangeAggregationRangeStep<F> {

	/**
	 * Add a bucket for the range {@code [lowerBound, upperBound)} (lower bound included, upper bound excluded),
	 * or {@code (lowerBound, upperBound)} (both bounds excluded) if the lower bound is {@code -Infinity}.
	 *
	 * @param lowerBound The lower bound of the range.
	 * @param upperBound The upper bound of the range.
	 * @return The next step.
	 */
	default RangeAggregationRangeMoreStep<F> range(F lowerBound, F upperBound) {
		return range( Range.of( lowerBound, upperBound ) );
	}

	/**
	 * Add a bucket for given range.
	 *
	 * @param range The range to add.
	 * @return The next step.
	 *
	 * @see Range
	 */
	RangeAggregationRangeMoreStep<F> range(Range<? extends F> range);

	/**
	 * Add one bucket for each of the given ranges.
	 *
	 * @param ranges The ranges to add.
	 * @return The next step.
	 *
	 * @see Range
	 */
	RangeAggregationRangeMoreStep<F> ranges(Collection<? extends Range<? extends F>> ranges);

}
