package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.scorefunction.FieldValueFactorFieldReference;

/**
 * The step in a "function score" predicate definition where score functions can be added.
 * <p>
 * Follows the same flow pattern as {@link BooleanPredicateClausesStep}: functions are clause methods,
 * config steps extend this step so you can break out to add another function at any point.
 * <p>
 * If no functions are added, the inner predicate's score is preserved, regardless of the boost mode.
 * Otherwise, functions that match the document are combined using the score mode, with a score of
 * {@code 1} when none match. The combined function score is capped at {@link Float#MAX_VALUE}
 * before applying the boost mode. The final score can overflow to positive infinity.
 * A predicate {@link #boost(float) boost} is applied after combining the function and inner predicate scores.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface FunctionScoreClausesStep<SR, S extends FunctionScoreClausesStep<SR, S>>
		extends PredicateFinalStep, PredicateScoreStep<S> {

	/**
	 * Add a field value factor function that reads a numeric field value to influence the score.
	 *
	 * @param fieldPath The path to the numeric field.
	 * @return The next step, where the function can be configured.
	 */
	FunctionScoreFieldValueFactorStep<SR, S> fieldValueFactor(String fieldPath);

	/**
	 * Add a field value factor function that reads a numeric field value to influence the score.
	 *
	 * @param fieldReference The type-safe reference to the numeric field.
	 * @return The next step, where the function can be configured.
	 * @param <T> The field value type.
	 */
	<T> FunctionScoreFieldValueFactorStep<SR, S> fieldValueFactor(
			FieldValueFactorFieldReference<? super SR, T> fieldReference);

	/**
	 * Add a standalone weight function that produces a constant score.
	 * <p>
	 * In Elasticsearch terms, this is the {@code weight} function: it produces a base score
	 * of 1.0 with a per-function weight equal to the given value.
	 *
	 * @param weight The weight value.
	 * @return The next step, where a filter can optionally be set.
	 */
	FunctionScoreWeightStep<SR, S> weight(float weight);

	/**
	 * Set the score mode, which defines how scores from multiple functions are combined.
	 *
	 * @return The next step, where the score mode can be selected.
	 */
	FunctionScoreScoreModeStep<S> scoreMode();

	/**
	 * Set the boost mode, which defines how the combined function score is merged
	 * with the original query score.
	 *
	 * @return The next step, where the boost mode can be selected.
	 */
	FunctionScoreBoostModeStep<S> boostMode();

}
