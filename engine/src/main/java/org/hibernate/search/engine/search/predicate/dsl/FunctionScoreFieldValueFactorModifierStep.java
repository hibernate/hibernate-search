package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "field value factor" function definition where the modifier can be selected.
 * <p>
 * The modifier transforms the field value before it becomes the function's raw score.
 *
 * @param <N> The type of the next step (returned by each option method).
 */
public interface FunctionScoreFieldValueFactorModifierStep<N> {

	N none();

	N log();

	N log1p();

	N log2p();

	N ln();

	N ln1p();

	N ln2p();

	N square();

	N sqrt();

	N reciprocal();

}
