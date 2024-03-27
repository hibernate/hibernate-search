/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * The step in a fast vector highlighter definition where options can be set. Refer to your particular backend documentation
 * for more detailed information on the exposed settings.
 */
public interface HighlighterFastVectorHighlighterOptionsStep
		extends HighlighterOptionsStep<HighlighterFastVectorHighlighterOptionsStep> {

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
	 * By default, a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterStep#chars() character boundary scanner} is used.
	 *
	 * @return The next step in a highlighter definition exposing boundary scanner specific options.
	 */
	HighlighterBoundaryScannerTypeFastVectorHighlighterStep<
			? extends HighlighterFastVectorHighlighterOptionsStep> boundaryScanner();

	/**
	 * Specify how the text should be broken up into highlighting snippets.
	 * <p>
	 * By default, a {@link HighlighterBoundaryScannerTypeFastVectorHighlighterStep#chars() character boundary scanner} is used.
	 *
	 * @param boundaryScannerContributor A consumer that will configure a boundary scanner for this highlighter.
	 * Should generally be a lambda expression.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterFastVectorHighlighterOptionsStep boundaryScanner(
			Consumer<? super HighlighterBoundaryScannerTypeFastVectorHighlighterStep<?>> boundaryScannerContributor);
}
