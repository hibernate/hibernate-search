/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * A generic superinterface for "simple boolean predicate" DSL steps that involve collecting
 * <a href="SimpleBooleanPredicateClausesCollector.html#clauses">clauses</a>.
 * <p>
 * See also {@link PredicateScoreStep} or {@link PredicateFinalStep}.
 *
 * @param <S> The "self" type (the actual exposed type of this collector).
 * @param <C> The "collector" type (the type of collector passed to the consumer in {@link #with(Consumer)}).
 */
public interface GenericSimpleBooleanPredicateClausesStep<
		S extends C,
		C extends SimpleBooleanPredicateClausesCollector<?>>
		extends SimpleBooleanPredicateClausesCollector<C>, PredicateFinalStep {
	@Override
	default S add(PredicateFinalStep searchPredicate) {
		return add( searchPredicate.toPredicate() );
	}

	@Override
	S add(SearchPredicate searchPredicate);

	@Override
	S add(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

	@Override
	S with(Consumer<? super C> contributor);
}
