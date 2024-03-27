/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

import java.util.function.Consumer;

/**
 * The step in a unified highlighter definition where options can be set. Refer to your particular backend documentation
 * for more detailed information on the exposed settings.
 */
public interface HighlighterUnifiedOptionsStep
		extends HighlighterOptionsStep<HighlighterUnifiedOptionsStep> {

	/**
	 * Specify how the text should be broken up into highlighting snippets.
	 * <p>
	 * By default, a {@link HighlighterBoundaryScannerTypeStep#sentence() sentence boundary scanner} is used.
	 *
	 * @return The next step in a highlighter definition exposing boundary scanner specific options.
	 */
	HighlighterBoundaryScannerTypeStep<?, ? extends HighlighterUnifiedOptionsStep> boundaryScanner();

	/**
	 * Specify how the text should be broken up into highlighting snippets.
	 * <p>
	 * By default, a {@link HighlighterBoundaryScannerTypeStep#sentence() sentence boundary scanner} is used.
	 *
	 * @param boundaryScannerContributor A consumer that will configure a boundary scanner for this highlighter.
	 * Should generally be a lambda expression.
	 * @return The next step in a highlighter definition.
	 */
	HighlighterUnifiedOptionsStep boundaryScanner(
			Consumer<? super HighlighterBoundaryScannerTypeStep<?, ?>> boundaryScannerContributor);
}
