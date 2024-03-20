/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a "match" predicate definition where the value to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface MatchPredicateMatchingStep<N extends MatchPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match the given value.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert#YES}.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 *
	 * @see #matching(Object, ValueConvert)
	 */
	default N matching(Object value) {
		return matching( value, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to match the given value.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 *
	 * @see ValueConvert
	 */
	N matching(Object value, ValueConvert convert);

	/**
	 * Require at least one of the targeted fields to match the value that will be passed to a query via a query parameter.
	 * <p>
	 * Same as {@link #matching(Object)} but delaying the match value to be passed via a query parameter.
	 *
	 * @param parameterName The name of a query parameter representing the value to match.
	 * @return The next step.
	 */
	@Incubating
	default N matchingParam(String parameterName) {
		return matchingParam( parameterName, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to match the value that will be passed to a query via a query parameter.
	 * <p>
	 * Same as {@link #matching(Object, ValueConvert)} but delaying the match value to be passed via a query parameter.
	 *
	 * @param parameterName The name of a query parameter representing the value to match.
	 * @param convert Controls how the {@code parameter value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 *
	 * @see ValueConvert
	 */
	@Incubating
	N matchingParam(String parameterName, ValueConvert convert);

}
