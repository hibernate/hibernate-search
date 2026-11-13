package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * The step in a "function score" predicate definition where a field value factor function
 * can be configured.
 * <p>
 * Extends {@link FunctionScoreClausesStep} so that at any point, another function can be
 * added or options can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type of the parent clauses step.
 */
public interface FunctionScoreFieldValueFactorStep<SR, S extends FunctionScoreClausesStep<SR, S>>
		extends FunctionScoreClausesStep<SR, S> {

	/**
	 * Set the modifier that transforms the field value before it becomes the function's raw score.
	 *
	 * @return The next step, where the modifier can be selected.
	 */
	FunctionScoreFieldValueFactorModifierStep<? extends FunctionScoreFieldValueFactorStep<SR, S>> modifier();

	/**
	 * Set the factor that multiplies the field value before the modifier is applied.
	 * <p>
	 * The formula is: {@code modifier(factor * fieldValue)}.
	 *
	 * @param factor The factor value. Default is 1.
	 * @return {@code this}, for method chaining.
	 */
	FunctionScoreFieldValueFactorStep<SR, S> factor(float factor);

	/**
	 * Set the value to use when a document does not have the target field.
	 * <p>
	 * The modifier and factor are still applied to this value.
	 * Uses the value model of the typed field reference for value conversion,
	 * or {@link ValueModel#MAPPING} when targeting the field by its path.
	 *
	 * @param value The missing value.
	 * @return {@code this}, for method chaining.
	 */
	FunctionScoreFieldValueFactorStep<SR, S> missing(Object value);

	/**
	 * Set the value to use when a document does not have the target field.
	 * <p>
	 * The modifier and factor are still applied to this value.
	 *
	 * @param value The missing value.
	 * @param valueModel The value model controlling how the value is converted.
	 * @return {@code this}, for method chaining.
	 */
	FunctionScoreFieldValueFactorStep<SR, S> missing(Object value, ValueModel valueModel);

	/**
	 * Set the per-function weight, which multiplies the function's raw score.
	 * <p>
	 * This weight also participates in the weighted average when using
	 * {@link FunctionScoreScoreModeStep#avg()}.
	 *
	 * @param weight The weight value. Default is 1.
	 * @return {@code this}, for method chaining.
	 */
	FunctionScoreFieldValueFactorStep<SR, S> functionWeight(float weight);

	/**
	 * Set a filter that restricts which documents this function applies to.
	 *
	 * @param filter The filter predicate.
	 * @return {@code this}, for method chaining.
	 */
	FunctionScoreFieldValueFactorStep<SR, S> filter(SearchPredicate filter);

	/**
	 * Set a filter that restricts which documents this function applies to.
	 *
	 * @param filterContributor A function that will define the filter predicate.
	 * @return {@code this}, for method chaining.
	 */
	FunctionScoreFieldValueFactorStep<SR, S> filter(
			Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> filterContributor);

}
