/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial;

import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * Minimum interface for a field/method to be spatial hash indexable
 *
 * @author Nicolas Helleringer
 * @deprecated Use {@link GeoPoint} instead.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface Coordinates {
	/**
	 * @return the latitude in degrees
	 */
	@Latitude
	Double getLatitude();

	/**
	 * @return the longitude in degrees
	 */
	@Longitude
	Double getLongitude();

	static GeoPoint toGeoPoint(Coordinates coordinates) {
		if ( coordinates == null ) {
			return null;
		}
		Double latitude = coordinates.getLatitude();
		Double longitude = coordinates.getLongitude();
		if ( latitude == null || longitude == null ) {
			return null;
		}
		return GeoPoint.of( latitude, longitude );
	}
}
