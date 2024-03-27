/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * @return the longitude, in degrees
	 */
	double longitude();

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
