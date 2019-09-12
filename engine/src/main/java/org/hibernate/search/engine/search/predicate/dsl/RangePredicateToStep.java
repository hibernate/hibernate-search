/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.common.ValueConvert;

/**
 * The step in a "range" predicate definition where the upper limit of the range can be set.
 */
public interface RangePredicateToStep {

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * in addition to being "higher than" the value provided to the
	 * former <code>{@link RangePredicateFieldMoreStep#from(Object) from}</code> call.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert#YES}.
	 *
	 * @param value The upper bound of the range. May be null, in which case the range has no upper bound,
	 * but this is only possible if the lower bound ({@link RangePredicateFieldMoreStep#from(Object)})
	 * was not null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * The upper bound is included in the range by default,
	 * but can be excluded by calling {@link RangePredicateLimitExcludeStep#excludeLimit()} on the next step.
	 * @return The next step.
	 */
	default RangePredicateLastLimitExcludeStep to(Object value) {
		return to( value, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * in addition to being "higher than" the value provided to the
	 * former <code>{@link RangePredicateFieldMoreStep#from(Object) from}</code> call.
	 *
	 * @param value The upper bound of the range. May be null, in which case the range has no upper bound,
	 * but this is only possible if the lower bound ({@link RangePredicateFieldMoreStep#from(Object)})
	 * was not null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link ValueConvert} for more information.
	 * The upper bound is included in the range by default,
	 * but can be excluded by calling {@link RangePredicateLimitExcludeStep#excludeLimit()} on the next step.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	RangePredicateLastLimitExcludeStep to(Object value, ValueConvert convert);

}
