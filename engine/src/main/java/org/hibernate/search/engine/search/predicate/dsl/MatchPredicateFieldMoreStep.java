/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "match" predicate definition where the value to match can be set
 * (see the superinterface {@link MatchPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface MatchPredicateFieldMoreStep<
		E,
		S extends MatchPredicateFieldMoreStep<E, ?, N>,
		N extends MatchPredicateOptionsStep<?>>
		extends MatchPredicateMatchingStep<N>, MatchPredicateFieldMoreGenericStep<E, S, N, Object, String> {

}
