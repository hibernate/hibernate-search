/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
