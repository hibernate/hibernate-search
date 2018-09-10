/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search;

import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;

public final class ProjectionConstants {

	private static final String PREFIX = "__HSEARCH_PROJECTION_";

	private ProjectionConstants() {
		// Private constructor, this is a utility class
	}

	/**
	 * Project to an object representing the match.
	 * <p>
	 * The actual type of the reference depends on the mapper used to create the query:
	 * a POJO mapper may return the POJO mapped to the index, for example.
	 * <p>
	 * As a general rule, a projection on {@link #OBJECT} will result in the same value
	 * which would have been returned by the query if not using projections
	 * (i.e. if {@link SearchQueryResultDefinitionContext#asObjects()} was called instead of
	 * {@link SearchQueryResultDefinitionContext#asProjections(String...)}).
	 */
	public static final String OBJECT = PREFIX + "object";

	/**
	 * Project to a reference to the match.
	 * <p>
	 * The actual type of the reference depends on the mapper used to create the query:
	 * a POJO mapper may return a class/identifier couple, for example.
	 * <p>
	 * As a general rule, a projection on {@link #REFERENCE} will result in the same value
	 * which would have been returned by the query if not using projections
	 * (i.e. if {@link SearchQueryResultDefinitionContext#asReferences()} was called instead of
	 * {@link SearchQueryResultDefinitionContext#asProjections(String...)}).
	 */
	public static final String REFERENCE = PREFIX + "reference";

	/**
	 * Project the match to a {@link DocumentReference}.
	 */
	public static final String DOCUMENT_REFERENCE = PREFIX + "document_reference";

}
