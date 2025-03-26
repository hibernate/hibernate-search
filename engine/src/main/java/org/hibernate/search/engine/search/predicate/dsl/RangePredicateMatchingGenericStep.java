/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

/**
 * The step in a "range" predicate definition where the range to match can be set.
 *
 * @param <N> The type of the next step.
 * @param <T> The type of the boundary values.
 */
public interface RangePredicateMatchingGenericStep<T, N extends RangePredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBound The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * @param upperBound The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * @return The next step.
	 */
	default N between(T lowerBound, T upperBound) {
		return within( Range.between( lowerBound, upperBound ) );
	}

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBound The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * @param lowerBoundInclusion Whether the lower bound is included in the range or excluded.
	 * @param upperBound The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * @param upperBoundInclusion Whether the upper bound is included in the range or excluded.
	 * @return The next step.
	 */
	default N between(T lowerBound, RangeBoundInclusion lowerBoundInclusion,
			T upperBound, RangeBoundInclusion upperBoundInclusion) {
		return within( Range.between( lowerBound, lowerBoundInclusion, upperBound, upperBoundInclusion ) );
	}

	/**
	 * Require at least one of the targeted fields to be "greater than or equal to" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * @return The next step.
	 */
	default N atLeast(T lowerBoundValue) {
		return within( Range.atLeast( lowerBoundValue ) );
	}

	/**
	 * Require at least one of the targeted fields to be "strictly greater than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * @return The next step.
	 */
	default N greaterThan(T lowerBoundValue) {
		return within( Range.greaterThan( lowerBoundValue ) );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than or equal to" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * @return The next step.
	 */
	default N atMost(T upperBoundValue) {
		return within( Range.atMost( upperBoundValue ) );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * @return The next step.
	 */
	default N lessThan(T upperBoundValue) {
		return within( Range.lessThan( upperBoundValue ) );
	}

	/**
	 * Require at least one of the targeted fields to be in the given range.
	 *
	 * @param range The range to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field.
	 * @return The next step.
	 */
	N within(Range<? extends T> range);

	/**
	 * Require at least one of the targeted fields to be in any of the given ranges.
	 *
	 * @param ranges The ranges to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N withinAny(Range<?>... ranges) {
		return withinAny( Arrays.asList( ranges ) );
	}

	/**
	 * Require at least one of the targeted fields to be in any of the given ranges.
	 *
	 * @param ranges The ranges to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N withinAny(Collection<? extends Range<?>> ranges) {
		return withinAny( ranges, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to be in any of the given ranges.
	 *
	 * @param ranges The ranges to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the range bounds should be converted
	 * before Hibernate Search attempts to interpret them as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	N withinAny(Collection<? extends Range<?>> ranges, ValueModel valueModel);
}
