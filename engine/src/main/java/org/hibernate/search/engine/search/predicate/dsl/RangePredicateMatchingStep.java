/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

/**
 * The step in a "range" predicate definition where the range to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface RangePredicateMatchingStep<N extends RangePredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBound The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @param upperBound The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N between(Object lowerBound, Object upperBound) {
		return between( lowerBound, upperBound, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBound The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param upperBound The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how {@code lowerBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	default N between(Object lowerBound, Object upperBound, ValueConvert convert) {
		return range( Range.between( lowerBound, upperBound ), convert );
	}

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBound The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @param lowerBoundInclusion Whether the lower bound is included in the range or excluded.
	 * @param upperBound The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @param upperBoundInclusion Whether the upper bound is included in the range or excluded.
	 * @return The next step.
	 */
	default N between(Object lowerBound, RangeBoundInclusion lowerBoundInclusion,
			Object upperBound, RangeBoundInclusion upperBoundInclusion) {
		return range( Range.between( lowerBound, lowerBoundInclusion, upperBound, upperBoundInclusion ) );
	}

	/**
	 * Require at least one of the targeted fields to be "greater than or equal to" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N atLeast(Object lowerBoundValue) {
		return atLeast( lowerBoundValue, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to be "greater than or equal to" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how {@code lowerBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	default N atLeast(Object lowerBoundValue, ValueConvert convert) {
		return range( Range.atLeast( lowerBoundValue ), convert );
	}

	/**
	 * Require at least one of the targeted fields to be "strictly greater than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N greaterThan(Object lowerBoundValue) {
		return greaterThan( lowerBoundValue, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to be "strictly greater than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how {@code lowerBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	default N greaterThan(Object lowerBoundValue, ValueConvert convert) {
		return range( Range.greaterThan( lowerBoundValue ), convert );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than or equal to" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N atMost(Object upperBoundValue) {
		return atMost( upperBoundValue, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than or equal to" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how {@code upperBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	default N atMost(Object upperBoundValue, ValueConvert convert) {
		return range( Range.atMost( upperBoundValue ), convert );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N lessThan(Object upperBoundValue) {
		return lessThan( upperBoundValue, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how {@code upperBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	default N lessThan(Object upperBoundValue, ValueConvert convert) {
		return range( Range.lessThan( upperBoundValue ), convert );
	}

	/**
	 * Require at least one of the targeted fields to be in the given range.
	 *
	 * @param range The range to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N range(Range<?> range) {
		return range( range, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to be in the given range.
	 *
	 * @param range The range to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how the range bounds should be converted
	 * before Hibernate Search attempts to interpret them as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	N range(Range<?> range, ValueConvert convert);

}
