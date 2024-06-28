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
 */
public interface RangePredicateMatchingStep<N extends RangePredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBound The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @param upperBound The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N between(Object lowerBound, Object upperBound) {
		return between( lowerBound, upperBound, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBoundValue The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param upperBoundValue The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how the {@code lowerBoundValue}/{@code upperBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #between(Object, Object, ValueModel)} instead.
	 */
	@Deprecated
	default N between(Object lowerBoundValue, Object upperBoundValue,
			org.hibernate.search.engine.search.common.ValueConvert convert) {
		return between( Range.between( lowerBoundValue, upperBoundValue ),
				org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBoundValue The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param upperBoundValue The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the {@code lowerBoundValue}/{@code upperBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	default N between(Object lowerBoundValue, Object upperBoundValue, ValueModel valueModel) {
		return within( Range.between( lowerBoundValue, upperBoundValue ), valueModel );
	}

	/**
	 * Require at least one of the targeted fields to be in the range
	 * defined by the given bounds.
	 *
	 * @param lowerBound The lower bound of the range. {@code null} means {@code -Infinity} (no lower bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @param lowerBoundInclusion Whether the lower bound is included in the range or excluded.
	 * @param upperBound The upper bound of the range. {@code null} means {@code +Infinity} (no upper bound).
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @param upperBoundInclusion Whether the upper bound is included in the range or excluded.
	 * @return The next step.
	 */
	default N between(Object lowerBound, RangeBoundInclusion lowerBoundInclusion,
			Object upperBound, RangeBoundInclusion upperBoundInclusion) {
		return within( Range.between( lowerBound, lowerBoundInclusion, upperBound, upperBoundInclusion ) );
	}

	/**
	 * Require at least one of the targeted fields to be "greater than or equal to" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N atLeast(Object lowerBoundValue) {
		return atLeast( lowerBoundValue, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to be "greater than or equal to" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how {@code lowerBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #atLeast(Object, ValueModel)} instead.
	 */
	@Deprecated
	default N atLeast(Object lowerBoundValue, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return within( Range.atLeast( lowerBoundValue ),
				org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to be "greater than or equal to" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the {@code lowerBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	default N atLeast(Object lowerBoundValue, ValueModel valueModel) {
		return within( Range.atLeast( lowerBoundValue ), valueModel );
	}

	/**
	 * Require at least one of the targeted fields to be "strictly greater than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N greaterThan(Object lowerBoundValue) {
		return greaterThan( lowerBoundValue, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to be "strictly greater than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how {@code lowerBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #greaterThan(Object, ValueModel)} instead.
	 */
	@Deprecated
	default N greaterThan(Object lowerBoundValue, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return within( Range.greaterThan( lowerBoundValue ),
				org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to be "strictly greater than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param lowerBoundValue The lower bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the {@code lowerBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	default N greaterThan(Object lowerBoundValue, ValueModel valueModel) {
		return within( Range.greaterThan( lowerBoundValue ), valueModel );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than or equal to" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N atMost(Object upperBoundValue) {
		return atMost( upperBoundValue, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than or equal to" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how {@code upperBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #atMost(Object, ValueModel)} instead.
	 */
	@Deprecated
	default N atMost(Object upperBoundValue, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return within( Range.atMost( upperBoundValue ),
				org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than or equal to" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, included. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the {@code upperBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	default N atMost(Object upperBoundValue, ValueModel valueModel) {
		return within( Range.atMost( upperBoundValue ), valueModel );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N lessThan(Object upperBoundValue) {
		return lessThan( upperBoundValue, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how {@code upperBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 */
	@Deprecated
	default N lessThan(Object upperBoundValue, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return within( Range.lessThan( upperBoundValue ),
				org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to be "lesser than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param upperBoundValue The upper bound of the range, excluded. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code valueModel} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the {@code upperBoundValue} should be converted
	 * before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	default N lessThan(Object upperBoundValue, ValueModel valueModel) {
		return within( Range.lessThan( upperBoundValue ), valueModel );
	}

	/**
	 * Require at least one of the targeted fields to be in the given range.
	 *
	 * @param range The range to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #within(Range)} instead.
	 */
	@Deprecated
	default N range(Range<?> range) {
		return within( range, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to be in the given range.
	 *
	 * @param range The range to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how the range bounds should be converted
	 * before Hibernate Search attempts to interpret them as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #within(Range, org.hibernate.search.engine.search.common.ValueConvert)} instead.
	 */
	@Deprecated
	default N range(Range<?> range, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return within( range, convert );
	}

	/**
	 * Require at least one of the targeted fields to be in the given range.
	 *
	 * @param range The range to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueModel#MAPPING} for more information.
	 * @return The next step.
	 */
	default N within(Range<?> range) {
		return within( range, ValueModel.MAPPING );
	}

	/**
	 * Require at least one of the targeted fields to be in the given range.
	 *
	 * @param range The range to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how the range bounds should be converted
	 * before Hibernate Search attempts to interpret them as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #within(Range, ValueModel)} instead.
	 */
	@Deprecated
	default N within(Range<?> range, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return within( range, org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Require at least one of the targeted fields to be in the given range.
	 *
	 * @param range The range to match.
	 * The signature of this method defines this parameter as a range with bounds of any type,
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueModel} for more information.
	 * @param valueModel The model value, determines how the range bounds should be converted
	 * before Hibernate Search attempts to interpret them as a field value.
	 * See {@link ValueModel} for more information.
	 * @return The next step.
	 */
	N within(Range<?> range, ValueModel valueModel);

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
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @param convert Controls how the range bounds should be converted
	 * before Hibernate Search attempts to interpret them as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert} for more information.
	 * @return The next step.
	 * @deprecated Use {@link #withinAny(Collection, ValueModel)} instead.
	 */
	@Deprecated
	default N withinAny(Collection<? extends Range<?>> ranges, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return withinAny( ranges, org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
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
