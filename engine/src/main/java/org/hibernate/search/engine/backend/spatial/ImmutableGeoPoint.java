/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.spatial;

import java.util.Objects;

public final class ImmutableGeoPoint implements GeoPoint {

	private final double latitude;
	private final double longitude;

	public ImmutableGeoPoint(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public String toString() {
		return "ImmutableGeoPoint["
				+ "latitude=" + latitude
				+ ", longitude=" + longitude
				+ "]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ImmutableGeoPoint that = (ImmutableGeoPoint) o;
		return that.latitude == latitude &&
				that.longitude == longitude;
	}

	@Override
	public int hashCode() {
		return Objects.hash( latitude, longitude );
	}

	@Override
	public double getLatitude() {
		return latitude;
	}

	@Override
	public double getLongitude() {
		return longitude;
	}

}
