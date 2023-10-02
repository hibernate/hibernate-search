/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.spatial.geopointbinding.property;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;

//tag::include[]
@Embeddable
public class MyCoordinates { // <1>

	@Basic
	@Latitude // <2>
	private Double latitude;

	@Basic
	@Longitude // <3>
	private Double longitude;

	protected MyCoordinates() {
		// For Hibernate ORM
	}

	public MyCoordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) { // <4>
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
}
//end::include[]
