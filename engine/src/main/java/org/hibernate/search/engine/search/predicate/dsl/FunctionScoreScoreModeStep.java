package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "function score" predicate definition where the score mode can be selected.
 * <p>
 * The score mode defines how scores from multiple functions are combined with each other.
 *
 * @param <N> The type of the next step (returned by each option method).
 */
public interface FunctionScoreScoreModeStep<N> {

	N multiply();

	/**
	 * Sum the weighted function scores, or use {@code 1} if the matching weights sum to zero.
	 * @return The next step.
	 */
	N sum();

	/**
	 * Divide the sum of weighted function scores by the sum of matching weights.
	 * This also applies to a single function. If the matching weights sum to zero, use {@code 1}.
	 * @return The next step.
	 */
	N avg();

	N first();

	N max();

	N min();

}
