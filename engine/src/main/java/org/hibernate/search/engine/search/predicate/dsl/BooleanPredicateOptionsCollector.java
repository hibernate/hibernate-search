/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * An object where the clauses and options of a {@link TypedSearchPredicateFactory#bool() boolean predicate} can be set.
 * <p>
 * Different types of clauses have different effects, see below.
 *
 * <h2 id="must">"must" clauses</h2>
 * <p>
 * "must" clauses are required to match: if they don't match, then the boolean predicate will not match.
 * <p>
 * Matching "must" clauses are taken into account during score computation.
 *
 * <h2 id="mustnot">"must not" clauses</h2>
 * <p>
 * "must not" clauses are required to not match: if they match, then the boolean predicate will not match.
 * <p>
 * "must not" clauses are ignored during score computation.
 *
 * <h2 id="filter">"filter" clauses</h2>
 * <p>
 * "filter" clauses are required to match: if they don't match, then the boolean predicate will not match.
 * <p>
 * "filter" clauses are ignored during score computation,
 * and so are any clauses of boolean predicates contained in the filter clause (even "must" or "should" clauses).
 *
 * <h2 id="should">"should" clauses</h2>
 * <p>
 * "should" clauses may optionally match, and are required to match depending on the context.
 * <p>
 * Matching "should" clauses are taken into account during score computation.
 * <p>
 * The exact behavior of `should` clauses is as follows:
 * <ul>
 * <li>
 *     When there isn't any "must" clause nor any "filter" clause in the boolean predicate,
 *     then at least one "should" clause is required to match.
 *     Simply put, in this case, the "should" clauses
 *     <strong>behave as if there was an "OR" operator between each of them</strong>.
 * </li>
 * <li>
 *     When there is at least one "must" clause or one "filter" clause in the boolean predicate,
 *     then the "should" clauses are not required to match,
 *     and are simply used for scoring.
 * </li>
 * <li>
 *     This behavior can be changed by specifying
 *     <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch">"minimumShouldMatch" constraints</a>.
 * </li>
 * </ul>
 *
 * @param <S> The "self" type (the actual exposed type of this collector).
 */
public interface BooleanPredicateOptionsCollector<SR, S extends BooleanPredicateOptionsCollector<SR, ?>>
		extends CommonMinimumShouldMatchOptionsStep<S> {

	/**
	 * Delegates setting clauses and options to a given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param contributor A consumer that will add clauses and options to the collector that it consumes.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S with(Consumer<? super S> contributor);

	/**
	 * Add a <a href="#must">"must" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that must match.
	 * @return {@code this}, for method chaining.
	 */
	S must(SearchPredicate searchPredicate);

	/**
	 * Add a <a href="#mustnot">"must not" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that must not match.
	 * @return {@code this}, for method chaining.
	 */
	S mustNot(SearchPredicate searchPredicate);

	/**
	 * Add a <a href="#should">"should" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that should match.
	 * @return {@code this}, for method chaining.
	 */
	S should(SearchPredicate searchPredicate);

	/**
	 * Add a <a href="#filter">"filter" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that must match.
	 * @return {@code this}, for method chaining.
	 */
	S filter(SearchPredicate searchPredicate);

	/*
	 * Syntactic sugar allowing to skip the toPredicate() call by passing a PredicateFinalStep
	 * directly.
	 */

	/**
	 * Add a <a href="#must">"must" clause</a> based on an almost-built {@link SearchPredicate}.
	 *
	 * @param dslFinalStep A final step in the predicate DSL allowing the retrieval of a {@link SearchPredicate}.
	 * @return {@code this}, for method chaining.
	 */
	default S must(PredicateFinalStep dslFinalStep) {
		return must( dslFinalStep.toPredicate() );
	}

	/**
	 * Add a <a href="#mustnot">"must not" clause</a> based on an almost-built {@link SearchPredicate}.
	 *
	 * @param dslFinalStep A final step in the predicate DSL allowing the retrieval of a {@link SearchPredicate}.
	 * @return {@code this}, for method chaining.
	 */
	default S mustNot(PredicateFinalStep dslFinalStep) {
		return mustNot( dslFinalStep.toPredicate() );
	}

	/**
	 * Add a <a href="#should">"should" clause</a> based on an almost-built {@link SearchPredicate}.
	 *
	 * @param dslFinalStep A final step in the predicate DSL allowing the retrieval of a {@link SearchPredicate}.
	 * @return {@code this}, for method chaining.
	 */
	default S should(PredicateFinalStep dslFinalStep) {
		return should( dslFinalStep.toPredicate() );
	}

	/**
	 * Add a <a href="#filter">"filter" clause</a> based on an almost-built {@link SearchPredicate}.
	 *
	 * @param dslFinalStep A final step in the predicate DSL allowing the retrieval of a {@link SearchPredicate}.
	 * @return {@code this}, for method chaining.
	 */
	default S filter(PredicateFinalStep dslFinalStep) {
		return filter( dslFinalStep.toPredicate() );
	}

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing the structure of the predicate building code to mirror the structure of predicates,
	 * even for complex predicate building requiring for example if/else statements.
	 */

	/**
	 * Add a <a href="#must">"must" clause</a> to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S must(Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * Add a <a href="#mustnot">"must not" clause</a> to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S mustNot(Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * Add a <a href="#should">"should" clause</a> to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S should(Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * Add a <a href="#filter">"filter" clause</a> to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S filter(Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * Checks if this predicate contains at least one clause.
	 *
	 * @return {@code true} if any clauses were added, i.e. any of the {@code must(..)}/{@code should(..)}/{@code mustNot(..)}/{@code filter(..)}
	 * were called at least once, {@code false} otherwise.
	 */
	boolean hasClause();
}
