/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl;

import org.hibernate.search.spatial.Coordinates;

/**
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface WithinContext {

	/**
	 * Coordinate object representing the center of the search
	 * @param coordinates the spatial {@link Coordinates}
	 * @return the {@link SpatialTermination}
	 **/
	SpatialTermination ofCoordinates(Coordinates coordinates);

	/**
	 * Latitude in degree
	 * @param latitude the latitude value
	 * @return the {@link LongitudeContext}
	 * */
	LongitudeContext ofLatitude(double latitude);

	interface LongitudeContext {

		/**
		 * Longitude in degree
		 * @param longitude the longiture value
		 * @return the {@link SpatialTermination}
		 * */
		SpatialTermination andLongitude(double longitude);
	}
}
