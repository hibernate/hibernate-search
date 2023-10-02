/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.spatial.genericfield;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

import org.hibernate.search.engine.spatial.GeoPoint;

//tag::include[]
@Embeddable
public class MyCoordinates implements GeoPoint { // <1>

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

	@Override
	public double latitude() { // <2>
		return latitude;
	}

	@Override
	public double longitude() {
		return longitude;
	}
}
//end::include[]
