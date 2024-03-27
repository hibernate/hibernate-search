/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * The final step in a highlighter definition where all boundary scanner options are already set.
 * Refer to your particular backend documentation for more detailed information on the exposed settings.
 */
public interface HighlighterBoundaryScannerFinalStep<T extends HighlighterOptionsStep<?>> {

	/**
	 * End the definition of a boundary scanner.
	 *
	 * @return The next step in a highlighter definition.
	 */
	T end();

}
