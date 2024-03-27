/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

import java.text.BreakIterator;

/**
 * The step in a highlighter definition where the boundary scanner type can be set.
 * Refer to your particular backend documentation for more detailed information on the exposed settings.
 */
public interface HighlighterBoundaryScannerTypeStep<
		T extends HighlighterBoundaryScannerOptionsStep<T, N>,
		N extends HighlighterOptionsStep<?>> {

	/**
	 * Break highlighted fragments at the next sentence boundary, as determined by {@link BreakIterator}.
	 *
	 * @return The next step in a highlighter definition.
	 */
	T sentence();

	/**
	 * Break highlighted fragments at the next word boundary, as determined by {@link BreakIterator}.
	 *
	 * @return The next step in a highlighter definition.
	 */
	T word();

}
