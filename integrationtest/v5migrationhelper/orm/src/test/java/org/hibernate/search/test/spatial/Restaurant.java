/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.spatial;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * Hibernate Search spatial : Point Of Interest test entity
 *
 * @author Nicolas Helleringer
 */
@Entity
@Indexed
public class Restaurant {
	@Id
	Integer id;

	@Field(store = Store.YES)
	String name;

	@IndexedEmbedded
	Position position;

	public Restaurant(Integer id, String name, String address, double latitude, double longitude) {
		this.id = id;
		this.name = name;
		this.position = new Position();
		this.position.address = address;
		this.position.latitude = latitude;
		this.position.longitude = longitude;
	}

	public Restaurant() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return position.address;
	}

	public Double getLatitude() {
		return position.latitude;
	}

	public Double getLongitude() {
		return position.longitude;
	}

}
