/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
