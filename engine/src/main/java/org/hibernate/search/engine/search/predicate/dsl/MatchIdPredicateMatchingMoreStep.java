/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in a "match id" predicate definition,
 * where more IDs to match can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface MatchIdPredicateMatchingMoreStep<
		S extends MatchIdPredicateMatchingMoreStep<?, N>,
		N extends MatchIdPredicateOptionsStep<?>>
		extends MatchIdPredicateMatchingStep<S>, MatchIdPredicateOptionsStep<N> {

}
