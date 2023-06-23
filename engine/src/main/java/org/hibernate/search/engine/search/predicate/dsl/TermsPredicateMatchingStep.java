/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Collection;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.impl.CollectionHelper;

/**
 * The step in a "terms" predicate definition where the terms to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface TermsPredicateMatchingStep<N extends TermsPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match <b>any</b> of the provided terms.
	 *
	 * @param firstTerm The (first) term to match.
	 * @param otherTerms The others (optional) terms to match.
	 * The signature of this method defines these parameter as a {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N matchingAny(Object firstTerm, Object... otherTerms) {
		return matchingAny( CollectionHelper.asList( firstTerm, otherTerms ) );
	}

	/**
	 * Require at least one of the targeted fields to match <b>any</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N matchingAny(Collection<?> terms) {
		return matchingAny( terms, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to match <b>any</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	N matchingAny(Collection<?> terms, ValueConvert convert);

	/**
	 * Require at least one of the targeted fields to match <b>all</b> of the provided terms.
	 *
	 * @param firstTerm The (first) term to match.
	 * @param otherTerms The others (optional) terms to match.
	 * The signature of this method defines these parameter as a {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N matchingAll(Object firstTerm, Object... otherTerms) {
		return matchingAll( CollectionHelper.asList( firstTerm, otherTerms ) );
	}

	/**
	 * Require at least one of the targeted fields to match <b>all</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link ValueConvert#YES} for more information.
	 * @return The next step.
	 */
	default N matchingAll(Collection<?> terms) {
		return matchingAll( terms, ValueConvert.YES );
	}

	/**
	 * Require at least one of the targeted fields to match <b>all</b> of the provided terms.
	 *
	 * @param terms The terms to match.
	 * The signature of this method defines this parameter as a {@link Collection} of any {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code convert} parameter.
	 * See {@link ValueConvert} for more information.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert} for more information.
	 * @return The next step.
	 */
	N matchingAll(Collection<?> terms, ValueConvert convert);

}
