/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl;

import java.util.Collection;

/**
 * The step in a fast vector highlighter definition where options can be set. Refer to your particular backend documentation
 * for more detailed information on the exposed settings.
 */
public interface HighlighterFastVectorHighlighterOptionsStep
		extends HighlighterOptionsStep<HighlighterFastVectorHighlighterOptionsStep> {

	/**
	 * Specify how far to scan for {@link #boundaryChars(String) boundary characters} when
	 * a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStep#chars() characters boundary scanner} is used.
	 * <p>
	 * Specifying this value allows to include more text in the resulting fragment. After highlighter have highlighted a match
	 * and centered it based on the {@link #fragmentSize(int) fragment size}, it can additionally move the start/end
	 * positions of that fragment by looking for {@code max} characters to the left and to the right to find any boundary character.
	 * As soon as such character is found - it will become a new start/end position of the fragment. Otherwise,
	 * if boundary character is not found after moving for the {@code max} characters to the left/right - the original
	 * position determined after centering the match will be used.
	 *
	 * @param max The number of characters.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterFastVectorHighlighterOptionsStep boundaryMaxScan(int max);

	/**
	 * Specify a string of characters to look for when scanning for boundaries when
	 * a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStep#chars() characters boundary scanner} is used.
	 *
	 * @param boundaryChars The string of boundary characters.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterFastVectorHighlighterOptionsStep boundaryChars(String boundaryChars);

	/**
	 * Specify a string of characters to look for when scanning for boundaries when
	 * a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStep#chars() characters boundary scanner} is used.
	 *
	 * @param boundaryChars The array of boundary characters.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterFastVectorHighlighterOptionsStep boundaryChars(Character[] boundaryChars);

	/**
	 * Specify the maximum number of matching phrases in a document that are considered for highlighting.
	 *
	 * @param limit The maximum number of matching phrases.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterFastVectorHighlighterOptionsStep phraseLimit(int limit);

	/**
	 * An alternative to {@link #tag(String, String) tag definition}.
	 * <p>
	 * Any previous calls to {@link #tag(String, String)}/{@link #tags(Collection, String)}/{@link #tags(Collection, Collection)}/{@link #tagSchema(HighlighterTagSchema)}
	 * on this highlighter definition will be discarded and tags supplied here will be used.
	 *
	 * @param preTags The opening (pre) tags placed before the highlighted text.
	 * @param postTag The closing (post) tag placed after the highlighted text.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterFastVectorHighlighterOptionsStep tags(Collection<String> preTags, String postTag);

	/**
	 * An alternative to {@link #tag(String, String) tag definition}.
	 * <p>
	 * Any previous calls to {@link #tag(String, String)}/{@link #tags(Collection, String)}/{@link #tags(Collection, Collection)}/{@link #tagSchema(HighlighterTagSchema)}
	 * on this highlighter definition will be discarded and tags supplied here will be used.
	 *
	 * @param preTags The opening (pre) tags placed before the highlighted text.
	 * @param postTags The closing (post) tags placed after the highlighted text.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterFastVectorHighlighterOptionsStep tags(Collection<String> preTags, Collection<String> postTags);

	/**
	 * Specify a set of predefined tags instead of {@link #tag(String, String) manually supplying them}.
	 * <p>
	 * Any previous calls to {@link #tag(String, String)}/{@link #tags(Collection, String)}/{@link #tags(Collection, Collection)}
	 * on this highlighter definition will be discarded and tags from the predefined schema will be used.
	 *
	 * @param tagSchema The tag schema to apply.
	 * @return The next step in a highlighter definition.
	 *
	 * @see HighlighterTagSchema
	 */
	HighlighterFastVectorHighlighterOptionsStep tagSchema(HighlighterTagSchema tagSchema);

	/**
	 * Specify how the text should be broken up into highlighting snippets.
	 * <p>
	 * By default, a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStep#chars() character boundary scanner} is used.
	 * @return The next step in a highlighter definition exposing boundary scanner specific options.
	 */
	HighlighterBoundaryScannerTypeFastVectorHighlighterOptionsStep<? extends HighlighterFastVectorHighlighterOptionsStep> boundaryScanner();
}
