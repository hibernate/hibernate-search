/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in a "match id" predicate definition,
 * where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface MatchIdPredicateOptionsStep<S extends MatchIdPredicateOptionsStep<?>>
		extends PredicateFinalStep, PredicateScoreStep<S> {

}
