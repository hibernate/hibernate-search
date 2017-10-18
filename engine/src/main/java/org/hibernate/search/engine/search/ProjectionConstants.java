/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search;

import org.hibernate.search.engine.search.dsl.SearchResultDefinitionContext;

public final class ProjectionConstants {

	private static final String PREFIX = "__HibernateSearch_";

	private ProjectionConstants() {
		// Private constructor, this is a utility class
	}

	/**
	 * Project to a reference to the match.
	 * <p>
	 * The actual type of the reference depends on the entry point
	 * for your search query: an {@link org.hibernate.search.engine.backend.index.spi.IndexManager}
	 * will return a {@link DocumentReference},
	 * but a {@link org.hibernate.search.engine.common.SearchManager} may
	 * return an implementation-specific type.
	 * <p>
	 * As a general rule, a projection on {@link #REFERENCE} will result in the same value
	 * which would have been returned by the query if not using projections
	 * (i.e. if {@link SearchResultDefinitionContext#asReferences()} was called instead of
	 * {@link SearchResultDefinitionContext#asProjections(String...)}).
	 */
	public static final String REFERENCE = PREFIX + "reference";

	/**
	 * Project the match to a {@link DocumentReference}.
	 */
	public static final String DOCUMENT_REFERENCE = PREFIX + "document_reference";

}
