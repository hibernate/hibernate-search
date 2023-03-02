/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * The step in a highlighter definition where options can be set. Refer to your particular backend documentation
 * for more detailed information on the exposed settings.
 */
public interface HighlighterOptionsStep<T extends HighlighterOptionsStep<?>>
		extends HighlighterFinalStep {

	/**
	 * Specify the size of the highlighted fragments in characters.
	 *
	 * @param size The number of characters in highlighted fragments.
	 * @return The next step in a highlighter definition.
	 */
	T fragmentSize(int size);

	/**
	 * Specify the amount of text to be returned, starting at the beginning of the field
	 * if there are no matching fragments to highlight.
	 *
	 * @param size The number of characters to include in the returned snippet.
	 * @return The next step in a highlighter definition.
	 */
	T noMatchSize(int size);

	/**
	 * Specify the maximum number of highlighted snippets to be returned.
	 *
	 * @param number The number of snippets to return.
	 * @return The next step in a highlighter definition.
	 */
	T numberOfFragments(int number);

	/**
	 * Specify if the highlighted fragments should be ordered by score.
	 * By default, the fragments are returned in the order they are present in the field.
	 *
	 * @param enable The parameter to enable/disable score ordering.
	 * @return The next step in a highlighter definition.
	 */
	T orderByScore(boolean enable);

	/**
	 * Specify the tags to wrap the highlighted text. Can be a pair of an HTML tags as well as any sequence of characters.
	 * <p>
	 * In case this method is called multiple times on a single highlighter definition,
	 * then the last pair of supplied tags will be applied, as highlighters require only one pair of tags.
	 * <p>
	 * By default, highlighted text is wrapped using {@code <em>} and {@code </em>} tags.
	 *
	 * @param preTag The opening (pre) tag placed before the highlighted text.
	 * @param postTag The closing (post) tag placed after the highlighted text.
	 * @return The next step in a highlighter definition.
	 */
	T tag(String preTag, String postTag);

	/**
	 * Specify the encoder for the highlighted output.
	 *
	 * @param encoder The encoder to be applied.
	 * @return The next step in a highlighter definition.
	 *
	 * @see HighlighterEncoder
	 */
	T encoder(HighlighterEncoder encoder);

}
