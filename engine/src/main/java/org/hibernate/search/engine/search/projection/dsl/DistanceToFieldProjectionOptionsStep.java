/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.spatial.DistanceUnit;

/**
 * The initial and final step in a "distance to field" projection definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface DistanceToFieldProjectionOptionsStep<S extends DistanceToFieldProjectionOptionsStep<?>>
		extends ProjectionFinalStep<Double> {

	/**
	 * Defines the unit of the computed distance (default is meters).
	 *
	 * @param unit The unit.
	 * @return {@code this}, for method chaining.
	 */
	ProjectionFinalStep<Double> unit(DistanceUnit unit);
}
