/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterOptionsStep;

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
	 */
	SingleHighlightProjectionFinalStep single();

}
