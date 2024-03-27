/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
