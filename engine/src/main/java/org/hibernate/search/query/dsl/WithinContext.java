/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl;

import org.hibernate.search.spatial.Coordinates;

/**
 * @author Emmanuel Bernard
 */
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
