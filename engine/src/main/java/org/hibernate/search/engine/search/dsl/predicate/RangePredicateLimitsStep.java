/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.search.predicate.DslConverter;

/**
 * The step in a "range" predicate definition where the limits of the range to match can be set.
 */
public interface RangePredicateLimitsStep {

	/**
	 * Require at least one of the targeted fields to be "higher than" the given value,
	 * and "lower than" another value (to be provided in following calls).
	 * <p>
	 * This syntax is essentially used like this: {@code .from( lowerBound ).to( upperBound )}.
	 *
	 * @param value The lower bound of the range. May be null, in which case the range has no lower bound
	 * and the upper bound (passed to {@link RangePredicateFromToStep#to(Object)}) must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link DslConverter#ENABLED} for more information.
	 * The lower bound is included in the range by default,
	 * but can be excluded by calling {@link RangePredicateLimitExcludeStep#excludeLimit()} on the next step.
	 * @return The next step.
	 */
	default RangePredicateFromToStep from(Object value) {
		return from( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to be "higher than" the given value,
	 * and "lower than" another value (to be provided in following calls).
	 * <p>
	 * This syntax is essentially used like this: {@code .from( lowerBound ).to( upperBound )}.
	 *
	 * @param value The lower bound of the range. May be null, in which case the range has no lower bound
	 * and the upper bound (passed to {@link RangePredicateFromToStep#to(Object)}) must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link DslConverter} for more information.
	 * The lower bound is included in the range by default,
	 * but can be excluded by calling {@link RangePredicateLimitExcludeStep#excludeLimit()} on the next step.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter} for more information.
	 * @return The next step.
	 */
	RangePredicateFromToStep from(Object value, DslConverter dslConverter);

	/**
	 * Require at least one of the targeted fields to be "higher than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param value The lower bound of the range. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link DslConverter#ENABLED} for more information.
	 * The lower bound is included in the range by default,
	 * but can be excluded by calling {@link RangePredicateLimitExcludeStep#excludeLimit()} on the next step.
	 * @return The next step.
	 */
	default RangePredicateLastLimitExcludeStep above(Object value) {
		return above( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to be "higher than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param value The lower bound of the range. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link DslConverter} for more information.
	 * The lower bound is included in the range by default,
	 * but can be excluded by calling {@link RangePredicateLimitExcludeStep#excludeLimit()} on the next step.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter} for more information.
	 * @return The next step.
	 */
	RangePredicateLastLimitExcludeStep above(Object value, DslConverter dslConverter);

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param value The upper bound of the range. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link DslConverter#ENABLED} for more information.
	 * The upper bound is included in the range by default,
	 * but can be excluded by calling {@link RangePredicateLimitExcludeStep#excludeLimit()} on the next step.
	 * @return The next step.
	 */
	default RangePredicateLastLimitExcludeStep below(Object value) {
		return below( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param value The upper bound of the range. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link DslConverter} for more information.
	 * The upper bound is included in the range by default,
	 * but can be excluded by calling {@link RangePredicateLimitExcludeStep#excludeLimit()} on the next step.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter} for more information.
	 * @return The next step.
	 */
	RangePredicateLastLimitExcludeStep below(Object value, DslConverter dslConverter);

}
