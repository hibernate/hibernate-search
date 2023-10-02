/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.configuration;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.spatial.Coordinates;

@Entity
public class ClassLevelTestPoI implements Coordinates {
	@Id
	@GeneratedValue
	private int poiId;

	private String name;

	private Double latitude;
	private Double longitude;

	@Override
	public Double getLatitude() {
		return latitude;
	}

	@Override
	public Double getLongitude() {
		return longitude;
	}

	public int getPoiId() {
		return poiId;
	}

	public void setPoiId(int poiId) {
		this.poiId = poiId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ClassLevelTestPoI(String name, double latitude, double longitude) {
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public ClassLevelTestPoI() {
	}

}
