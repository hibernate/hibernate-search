/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * The step in a highlighter definition where the boundary scanner type can be set.
 * Refer to your particular backend documentation for more detailed information on the exposed settings.
 */
public interface HighlighterBoundaryScannerTypeFastVectorHighlighterStep<N extends HighlighterOptionsStep<?>>
		extends
		HighlighterBoundaryScannerTypeStep<HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<N>, N> {

	/**
	 * Break highlighted fragments at the next boundary based on a
	 * {@link HighlighterBoundaryScannerFastVectorHighlighterOptionsStep#boundaryChars(String) provided boundary characters}
	 *
	 * @return The next step in a highlighter definition.
	 */
	HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<N> chars();

}
