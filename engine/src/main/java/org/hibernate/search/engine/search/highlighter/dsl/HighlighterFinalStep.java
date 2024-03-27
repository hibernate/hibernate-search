/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

import org.hibernate.search.engine.search.highlighter.SearchHighlighter;

/**
 * The final step in highlighter definition.
 */
public interface HighlighterFinalStep {

	/**
	 * Create an instance of a {@link SearchHighlighter} matching the configuration applied in the previous steps of this DSL.
	 *
	 * @return The {@link SearchHighlighter} resulting from the previous DSL steps.
	 */
	SearchHighlighter toHighlighter();
}
