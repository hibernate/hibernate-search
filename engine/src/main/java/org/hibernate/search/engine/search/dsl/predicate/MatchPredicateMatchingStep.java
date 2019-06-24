/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.search.predicate.DslConverter;

/**
 * The step in a "match" predicate definition where the value to match can be set.
 */
public interface MatchPredicateMatchingStep {

	/**
	 * Require at least one of the targeted fields to match the given value.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter#ENABLED}.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link DslConverter#ENABLED} for more information.
	 * @return The next step.
	 *
	 * @see #matching(Object, DslConverter)
	 */
	default MatchPredicateOptionsStep matching(Object value) {
		return matching( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to match the given value.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link DslConverter} for more information.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter} for more information.
	 * @return The next step.
	 *
	 * @see DslConverter
	 */
	MatchPredicateOptionsStep matching(Object value, DslConverter dslConverter);

}
