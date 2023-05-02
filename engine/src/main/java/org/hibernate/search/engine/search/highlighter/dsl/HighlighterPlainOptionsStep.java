/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * The step in a plain highlighter definition where options can be set. Refer to your particular backend documentation
 * for more detailed information on the exposed settings.
 */
public interface HighlighterPlainOptionsStep
		extends HighlighterOptionsStep<HighlighterPlainOptionsStep> {

	/**
	 * Specify how the text should be broken up into highlighting snippets.
	 *
	 * @param type The type of fragmeter to be applied.
	 * @return The next step in a highlighter definition.
	 *
	 * @see HighlighterFragmenter
	 */
	HighlighterPlainOptionsStep fragmenter(HighlighterFragmenter type);
}
