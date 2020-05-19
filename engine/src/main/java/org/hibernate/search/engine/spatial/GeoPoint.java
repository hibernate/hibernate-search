/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spatial;

/**
 * A point in the geocentric coordinate system.
 *
 * @author Nicolas Helleringer
 */
public interface GeoPoint {
	/**
	 * @return the latitude, in degrees
	 */
	double latitude();

	/**
	 * @return the latitude, in degrees
	 * @deprecated Use {@link #latitude()} instead.
	 */
	@Deprecated
	default double getLatitude() {
		return latitude();
	}

	/**
	 * @return the longitude, in degrees
	 */
	double longitude();

	/**
	 * @return the longitude, in degrees
	 * @deprecated Use {@link #longitude()} instead.
	 */
	@Deprecated
	default double getLongitude() {
		return longitude();
	}

	/**
	 * Create a {@link GeoPoint} from a latitude and a longitude.
	 *
	 * @param latitude The latitude of the GeoPoint, in degrees.
	 * @param longitude The longitude of the GeoPoint, in degrees.
	 * @return The corresponding {@link GeoPoint}.
	 */
	static GeoPoint of(double latitude, double longitude) {
		return new ImmutableGeoPoint( latitude, longitude );
	}
}
