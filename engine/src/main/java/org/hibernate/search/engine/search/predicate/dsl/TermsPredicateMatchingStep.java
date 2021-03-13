/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "terms" predicate definition where the terms to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface TermsPredicateMatchingStep<N extends TermsPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match <b>any</b> of the provided terms.
	 *
	 * @param term The (first) term to match.
	 * @param terms The others (optional) terms to match.
	 * @return The next step.
	 */
	N matchingAny(Object term, Object ... terms);

	/**
	 * Require at least one of the targeted fields to match <b>all</b> of the provided terms.
	 *
	 * @param term The (first) term to match.
	 * @param terms The others (optional) terms to match.
	 * @return The next step.
	 */
	N matchingAll(Object term, Object ... terms);

}
