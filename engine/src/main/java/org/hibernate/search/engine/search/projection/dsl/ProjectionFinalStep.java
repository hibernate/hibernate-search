/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.search.projection.SearchProjection;

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
