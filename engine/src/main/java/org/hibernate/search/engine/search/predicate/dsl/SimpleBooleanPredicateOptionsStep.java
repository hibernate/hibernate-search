/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial and final step in a "simple boolean predicate" definition, where options can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface SimpleBooleanPredicateOptionsStep<S extends SimpleBooleanPredicateOptionsStep<?>>
		extends PredicateScoreStep<S>, PredicateFinalStep {

}
