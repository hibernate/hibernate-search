/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * The step in a unified highlighter definition where options can be set. Refer to your particular backend documentation
 * for more detailed information on the exposed settings.
 */
public interface HighlighterUnifiedOptionsStep
		extends HighlighterOptionsStep<HighlighterUnifiedOptionsStep> {

	/**
	 * Specify the maximum number of characters to be analyzed by the highlighter.
	 *
	 * @param max The maximum number of characters to consider when highlighting.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterUnifiedOptionsStep maxAnalyzedOffset(int max);

	/**
	 * Specify how the text should be broken up into highlighting snippets.
	 *
	 * By default, a {@link HighlighterBoundaryScannerTypeOptionsStep#sentence() sentence boundary scanner} is used.
	 * @return The next step in a highlighter definition exposing boundary scanner specific options.
	 */
	HighlighterBoundaryScannerTypeOptionsStep<? extends HighlighterUnifiedOptionsStep> boundaryScanner();
}
