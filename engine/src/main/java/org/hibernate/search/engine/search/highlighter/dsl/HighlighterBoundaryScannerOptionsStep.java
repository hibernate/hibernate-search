/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * The step in a highlighter definition where the boundary scanner options can be set.
 * Refer to your particular backend documentation for more detailed information on the exposed settings.
 */
public interface HighlighterBoundaryScannerOptionsStep<
		T extends HighlighterBoundaryScannerOptionsStep<?, ?>,
		N extends HighlighterOptionsStep<?>>
		extends HighlighterBoundaryScannerFinalStep<N> {

	/**
	 * Specify a locale to be used when searching for boundaries by a {@link BreakIterator}.
	 *
	 * @param locale The locale to be applied by the {@link BreakIterator}
	 * @return The next step in a highlighter definition.
	 */
	T locale(Locale locale);

}
