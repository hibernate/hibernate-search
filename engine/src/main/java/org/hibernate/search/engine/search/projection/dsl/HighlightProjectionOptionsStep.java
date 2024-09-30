/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterOptionsStep;
import org.hibernate.search.engine.search.projection.ProjectionAccumulator;

/**
 * The initial and final step in a highlight definition, where optional parameters can be set.
 */
public interface HighlightProjectionOptionsStep extends HighlightProjectionFinalStep {

	/**
	 * Defines a name of a named highlighter to be used by this field projection.
	 *
	 * @param highlighterName The name of a highlighter
	 * {@link org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#highlighter(String, Function) defined on the query}.
	 *
	 * @return A final step to finish the definition of a highlight projection.
	 *
	 * @see org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#highlighter(String, Function)
	 */
	HighlightProjectionOptionsStep highlighter(String highlighterName);

	/**
	 * Defines the projection as single-valued, i.e. returning {@code String} instead of {@code List<String>}.
	 * <p>
	 * Can only be used when the highlighter that creates highlighted fragments for this projection is configured
	 * to return a single fragment at most, i.e. when {@link HighlighterOptionsStep#numberOfFragments(int) .numberOfFragments(1)}
	 * is applied to the highlighter.
	 * Otherwise, it will lead to an exception being thrown when the query is created.
	 *
	 * @return A final step in the highlight projection definition.
	 * @see HighlighterOptionsStep#numberOfFragments(int)
	 * @deprecated Use the {@link #accumulator(ProjectionAccumulator.Provider)} instead, e.g. {@code .accumulator(ProjectionAccumulator.single())}
	 */
	@Deprecated(since = "8.0")
	SingleHighlightProjectionFinalStep single();

	/**
	 * Defines the accumulator to apply to the highlighted strings.
	 * <p>
	 * By default, highlighting results in a list {@code List<String>} of highlighted strings.
	 * This method allows changing the returned type to a different collection of strings, e.g. {@code Set<String>}/{@code String[]}
	 * or obtaining a single-valued projection (i.e. {@code projectionAccumulatorProvider.isSingleValued () == true}).
	 * <p>
	 * Note: single-valued projections can only be used when the highlighter
	 * that creates highlighted fragments for this projection is configured
	 * to return a single fragment at most, i.e. when {@link HighlighterOptionsStep#numberOfFragments(int) .numberOfFragments(1)}
	 * is applied to the highlighter.
	 * Otherwise, it will lead to an exception being thrown when the query is created.
	 *
	 * @param accumulator The accumulator provider to apply to this projection.
	 * @return A final step in the highlight projection definition.
	 * @param <R> The type of the final result.
	 */
	<R> ProjectionFinalStep<R> accumulator(ProjectionAccumulator.Provider<String, R> accumulator);
}
