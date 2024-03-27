/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.spatial.DistanceUnit;

/**
 * The initial and final step in a "distance to field" projection definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <T> The type of projected distances.
 */
public interface DistanceToFieldProjectionOptionsStep<S extends DistanceToFieldProjectionOptionsStep<?, T>, T>
		extends ProjectionFinalStep<T> {

	/**
	 * Defines the unit of the computed distance (default is meters).
	 *
	 * @param unit The unit.
	 * @return {@code this}, for method chaining.
	 */
	S unit(DistanceUnit unit);

}
