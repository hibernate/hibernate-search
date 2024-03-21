/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * The initial step in a "distance to the field" projection definition,
 * where the center from which the distance is computed.
 * @param <T> The type of projected distances.
 */
public interface DistanceToFieldProjectionFromStep<S extends DistanceToFieldProjectionFromStep<?, T>, T> {

	/**
	 * Defines the center from which the distance is computed from.
	 *
	 * @param center The center to compute the distance from.
	 * @return A new step to define optional parameters for the distance projection.
	 */
	DistanceToFieldProjectionValueStep<?, T> from(GeoPoint center);

	/**
	 * Defines the center from which the distance is computed from.
	 *
	 * @param parameterName The name of a query parameter representing the center to compute the distance from.
	 * @return A new step to define optional parameters for the distance projection.
	 */
	DistanceToFieldProjectionValueStep<?, T> fromParam(String parameterName);

}
