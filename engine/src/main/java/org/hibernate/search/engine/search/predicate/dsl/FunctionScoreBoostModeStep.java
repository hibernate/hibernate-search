package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "function score" predicate definition where the boost mode can be selected.
 * <p>
 * The boost mode defines how the combined function score is merged with the original query score.
 *
 * @param <N> The type of the next step (returned by each option method).
 */
public interface FunctionScoreBoostModeStep<N> {

	N multiply();

	N replace();

	N sum();

	N avg();

	N max();

	N min();

}
