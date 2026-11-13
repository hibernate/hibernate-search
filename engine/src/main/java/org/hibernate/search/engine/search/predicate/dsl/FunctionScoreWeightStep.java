package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * The step in a "function score" predicate definition where a standalone weight function
 * can be configured with an optional filter.
 * <p>
 * Extends {@link FunctionScoreClausesStep} so that at any point, another function can be
 * added or options can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type of the parent clauses step.
 */
public interface FunctionScoreWeightStep<SR, S extends FunctionScoreClausesStep<SR, S>>
		extends FunctionScoreClausesStep<SR, S> {

	/**
	 * Set a filter that restricts which documents this weight function applies to.
	 *
	 * @param filter The filter predicate.
	 * @return The parent clauses step, for method chaining.
	 */
	S filter(SearchPredicate filter);

	/**
	 * Set a filter that restricts which documents this weight function applies to.
	 *
	 * @param filterContributor A function that will define the filter predicate.
	 * @return The parent clauses step, for method chaining.
	 */
	S filter(Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> filterContributor);

}
