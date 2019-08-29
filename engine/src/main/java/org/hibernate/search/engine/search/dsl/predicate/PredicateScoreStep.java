/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The step in a predicate definition where score-related options can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step)
 */
public interface PredicateScoreStep<S> extends PredicateBoostStep<S> {

	/**
	 * Force the score of the predicate to a single constant, identical for all documents.
	 * <p>
	 * By default, the score will be {@code 1.0f},
	 * but {@link #boost(float) boosts}, if any, will still be applied to the predicate.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S constantScore();

	/**
	 * @deprecated Use {@link #constantScore()} instead.
	 * @return {@code this}, for method chaining.
	 */
	@Deprecated
	default S withConstantScore() {
		return constantScore();
	}

}
