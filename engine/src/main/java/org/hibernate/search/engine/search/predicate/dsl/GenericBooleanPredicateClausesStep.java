/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * A generic superinterface for Predicate DSL steps that involve collecting clauses and options of a boolean predicate.
 * <p>
 * This interface mostly a technical detail to handle generics in the predicate DSL;
 * refer to {@link BooleanPredicateOptionsCollector}, {@link PredicateScoreStep} or {@link PredicateFinalStep}
 * for meaningful documentation.
 *
 * @param <S> The "self" type (the actual exposed type of this collector).
 * @param <C> The "collector" type (the type of collector passed to the consumer in {@link #with(Consumer)}.
 */
public interface GenericBooleanPredicateClausesStep<
		S extends C,
		C extends BooleanPredicateOptionsCollector<?>>
		extends BooleanPredicateOptionsCollector<C>, PredicateScoreStep<S>, PredicateFinalStep {

	@Override
	S with(Consumer<? super C> contributor);

	@Override
	S must(SearchPredicate searchPredicate);

	@Override
	S mustNot(SearchPredicate searchPredicate);

	@Override
	S should(SearchPredicate searchPredicate);

	@Override
	S filter(SearchPredicate searchPredicate);

	@Override
	default S must(PredicateFinalStep dslFinalStep) {
		return must( dslFinalStep.toPredicate() );
	}

	@Override
	default S mustNot(PredicateFinalStep dslFinalStep) {
		return mustNot( dslFinalStep.toPredicate() );
	}

	@Override
	default S should(PredicateFinalStep dslFinalStep) {
		return should( dslFinalStep.toPredicate() );
	}

	@Override
	default S filter(PredicateFinalStep dslFinalStep) {
		return filter( dslFinalStep.toPredicate() );
	}

	@Override
	S must(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

	@Override
	S mustNot(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

	@Override
	S should(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

	@Override
	S filter(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

	@Override
	default S minimumShouldMatchNumber(int matchingClausesNumber) {
		return minimumShouldMatch()
				.ifMoreThan( 0 ).thenRequireNumber( matchingClausesNumber )
				.end();
	}

	@Override
	default S minimumShouldMatchPercent(int matchingClausesPercent) {
		return minimumShouldMatch()
				.ifMoreThan( 0 ).thenRequirePercent( matchingClausesPercent )
				.end();
	}

	@Override
	MinimumShouldMatchConditionStep<S> minimumShouldMatch();

	@Override
	S minimumShouldMatch(Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor);

}
