/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;


/**
 * The step in a predicate definition where score-related options can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step)
 */
public interface PredicateScoreStep<S> {

	/**
	 * Boost the weight of the predicate in score computation.
	 *
	 * @param boost The boost factor. Higher than 1 increases the weight in score computation,
	 * between 0 and 1 lowers the weight. Lower than 0 is for experts only.
	 * @return {@code this}, for method chaining.
	 */
	S boost(float boost);

	/**
	 * Force the score of the predicate to a single constant, identical for all documents.
	 * <p>
	 * By default, the score will be {@code 1.0f},
	 * but {@link #boost(float) boosts}, if any, will still be applied to the predicate.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S constantScore();

}
