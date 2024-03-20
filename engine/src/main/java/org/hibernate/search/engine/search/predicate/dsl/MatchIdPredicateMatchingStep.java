/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Collection;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a "match id" predicate definition where the IDs to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface MatchIdPredicateMatchingStep<N extends MatchIdPredicateMatchingMoreStep<?, ?>> {

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * @see #matchingAny(Collection)
	 * @param value the value of the id we want to match.
	 * @return The next step.
	 */
	default N matching(Object value) {
		return matching( value, ValueConvert.YES );
	}

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * @see #matchingAny(Collection)
	 * @param value the value of the id we want to match.
	 * @param convert Controls how the {@code value} should be converted
	 * before Hibernate Search attempts to interpret it as an identifier value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	N matching(Object value, ValueConvert convert);

	/**
	 * Target the identifiers matching any of the values in a collection.
	 * @param values the collection of identifiers to match.
	 * @return The next step.
	 */
	default N matchingAny(Collection<?> values) {
		return matchingAny( values, ValueConvert.YES );
	}

	/**
	 * Target the identifiers matching any of the values in a collection.
	 * @param values the collection of identifiers to match.
	 * @param convert Controls how the {@code value} should be converted
	 * before Hibernate Search attempts to interpret it as an identifier value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	default N matchingAny(Collection<?> values, ValueConvert convert) {
		N next = null;
		for ( Object value : values ) {
			next = matching( value, convert );
		}
		return next;
	}

	/**
	 * Target the identifier with the id that will be passed to a query via a query parameter .
	 * <p>
	 * Same as {@link #matching(Object)} but delaying the match value to be passed via a query parameter.
	 * <p>
	 * If used multiple times, it will target any of the specified parameter values.
	 *
	 * @param parameterName The name of a query parameter representing the id to match.
	 * @return The next step.
	 */
	@Incubating
	default N matchingParam(String parameterName) {
		return matchingParam( parameterName, ValueConvert.YES );
	}

	/**
	 * Target the identifier with the id that will be passed to a query via a query parameter .
	 * <p>
	 * Same as {@link #matching(Object, ValueConvert)} but delaying the match value to be passed via a query parameter.
	 * <p>
	 * If used multiple times, it will target any of the specified parameter values.
	 *
	 * @param parameterName The name of a query parameter representing the id to match.
	 * @param convert Controls how the parameter {@code value} should be converted
	 * before Hibernate Search attempts to interpret it as an identifier value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 *
	 * @see ValueConvert
	 */
	@Incubating
	N matchingParam(String parameterName, ValueConvert convert);
}
