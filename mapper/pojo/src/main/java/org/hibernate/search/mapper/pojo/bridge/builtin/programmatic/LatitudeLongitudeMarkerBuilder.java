/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.programmatic;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBuilder;

/**
 * A builder of markers that mark a property as the latitude or longitude
 * for a {@link GeoPointBinder GeoPoint bridge}.
 *
 * @see Latitude
 * @see Longitude
 * @see GeoPointBinder#latitude()
 * @see GeoPointBinder#longitude()
 */
public interface LatitudeLongitudeMarkerBuilder extends MarkerBuilder {

	/**
	 * @param markerSet The name of the "marker set".
	 * This is used to discriminate between multiple pairs of latitude/longitude markers.
	 * @return {@code this}, for method chaining.
	 * @see GeoPointBinder#markerSet(String)
	 */
	LatitudeLongitudeMarkerBuilder markerSet(String markerSet);

}
