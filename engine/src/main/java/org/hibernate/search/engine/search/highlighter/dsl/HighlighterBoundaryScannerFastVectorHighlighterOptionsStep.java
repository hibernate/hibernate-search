/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * The step in a highlighter definition where boundary scanner options can be set.
 * Refer to your particular backend documentation for more detailed information on the exposed settings.
 */
public interface HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<T extends HighlighterOptionsStep<?>>
		extends
		HighlighterBoundaryScannerOptionsStep<HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<T>, T> {

	/**
	 * Specify how far to scan for {@link #boundaryChars(String) boundary characters} when
	 * a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterStep#chars() characters boundary scanner} is used.
	 * <p>
	 * Specifying this value allows to include more text in the resulting fragment. After the highlighter highlighted a match
	 * and centered it based on the {@link HighlighterOptionsStep#fragmentSize(int) fragment size}, it can additionally move the start/end
	 * positions of that fragment by looking for {@code max} characters to the left and to the right to find any boundary character.
	 * As soon as such character is found, it will become a new start/end position of the fragment. Otherwise,
	 * if boundary character is not found after moving for the {@code max} characters to the left/right - the original
	 * position determined after centering the match will be used.
	 *
	 * @param max The number of characters.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<T> boundaryMaxScan(int max);

	/**
	 * Specify a set of characters to look for when scanning for boundaries when
	 * a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterStep#chars() characters boundary scanner} is used.
	 *
	 * @param boundaryChars A string containing all boundary characters. The order doesn't matter:
	 * each character in the string will be considered as a boundary character.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<T> boundaryChars(String boundaryChars);

	/**
	 * Specify a set of characters to look for when scanning for boundaries when
	 * a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterStep#chars() characters boundary scanner} is used.
	 *
	 * @param boundaryChars An array containing all boundary characters. The order doesn't matter:
	 * each character in the string will be considered as a boundary character.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterBoundaryScannerFastVectorHighlighterOptionsStep<T> boundaryChars(Character[] boundaryChars);

}
