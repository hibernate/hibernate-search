/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.search.predicate.DslConverter;

/**
 * The context used when defining a range predicate,
 * after the lower bound was provided but before the upper bound was provided.
 */
public interface RangePredicateFromContext {

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * in addition to being "higher than" the value provided to the
	 * former <code>{@link RangePredicateFieldSetContext#from(Object) from}</code> call.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter#ENABLED}.
	 *
	 * @param value The upper bound of the range. May be null, in which case the range has no upper bound,
	 * but this is only possible if the lower bound ({@link RangePredicateFieldSetContext#from(Object)})
	 * was not null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See <a href="SearchPredicateFactoryContext.html#commonconcepts-parametertype">there</a> for more information.
	 *
	 * Upper bound is included by default in the range.
	 *
	 * @return A context allowing to exclude the upper bound from the range, to set options or to get the resulting predicate.
	 */
	default RangePredicateLimitTerminalContext to(Object value) {
		return to( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * in addition to being "higher than" the value provided to the
	 * former <code>{@link RangePredicateFieldSetContext#from(Object) from}</code> call.
	 *
	 * @param value The upper bound of the range. May be null, in which case the range has no upper bound,
	 * but this is only possible if the lower bound ({@link RangePredicateFieldSetContext#from(Object)})
	 * was not null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See <a href="SearchPredicateFactoryContext.html#commonconcepts-parametertype">there</a> for more information.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter}.
	 *
	 * Upper bound is included by default in the range.
	 *
	 * @return A context allowing to exclude the upper bound from the range, to set options or to get the resulting predicate.
	 */
	RangePredicateLimitTerminalContext to(Object value, DslConverter dslConverter);

	/**
	 * Exclude the lower bound from the range.
	 *
	 * @return A context allowing to set the upper bound of the range.
	 */
	RangePredicateFromContext excludeLimit();

}
