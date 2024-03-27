/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.numeric;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

/**
 * @author Gunnar Morling
 */
@Indexed
class PointOfInterest {

	@DocumentId
	@Field(name = "myId")
	@NumericField
	private byte id;

	@Field(store = Store.YES)
	private double latitude;

	@Field(store = Store.YES)
	private Double longitude;

	PointOfInterest() {
	}

	public PointOfInterest(byte id, double latitude, Double longitude) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public byte getId() {
		return id;
	}

	public void setId(byte id) {
		this.id = id;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
}
