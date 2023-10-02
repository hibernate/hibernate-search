/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.bridgeresolver;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

// Let's assume this type cannot be changed to implement GeoPoint.
@Embeddable
public class MyCoordinates {

	@Basic
	private Double latitude;

	@Basic
	private Double longitude;

	protected MyCoordinates() {
		// For Hibernate ORM
	}

	public MyCoordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double latitude() { // <2>
		return latitude;
	}

	public double longitude() {
		return longitude;
	}
}
