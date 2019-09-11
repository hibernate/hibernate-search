/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.search.SearchProjection;

/**
 * The final step in a projection definition, where the projection can be retrieved.
 *
 * @param <T> The type returned by the projection.
 */
public interface ProjectionFinalStep<T> {

	/**
	 * Create a {@link SearchProjection} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchProjection} instance.
	 */
	SearchProjection<T> toProjection();

}
