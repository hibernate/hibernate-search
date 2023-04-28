/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.Function;

/**
 * The initial and final step in a highlight definition, where optional parameters can be set.
 */
public interface HighlightProjectionOptionsStep extends HighlightProjectionFinalStep {

	/**
	 * Defines a name of a named highlighter to be used by this field projection.
	 *
	 * @param highlighterName The name of a highlighter
	 * {@link org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#highlighter(String, Function) defined on the query}.
	 *
	 * @return A final step to finish the definition of a highlight projection.
	 *
	 * @see org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#highlighter(String, Function)
	 */
	HighlightProjectionFinalStep highlighter(String highlighterName);

}
