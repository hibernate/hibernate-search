/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
