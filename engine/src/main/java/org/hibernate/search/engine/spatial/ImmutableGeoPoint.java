/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.spatial;

import java.util.Objects;

final class ImmutableGeoPoint implements GeoPoint {

	private final double latitude;
	private final double longitude;

	ImmutableGeoPoint(double latitude, double longitude) {
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
		return that.latitude == latitude
				&& that.longitude == longitude;
	}

	@Override
	public int hashCode() {
		return Objects.hash( latitude, longitude );
	}

	@Override
	public double latitude() {
		return latitude;
	}

	@Override
	public double longitude() {
		return longitude;
	}

}
