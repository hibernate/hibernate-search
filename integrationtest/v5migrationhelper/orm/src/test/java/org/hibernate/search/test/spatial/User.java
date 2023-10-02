/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.spatial;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Spatial;

@Spatial(name = "home")
@Entity
@Indexed
@Table(name = "people") // User is a reserved name in most databases
public class User {

	@Id
	Integer id;

	@Latitude(of = "home")
	Double homeLatitude;

	@Longitude(of = "home")
	Double homeLongitude;

	public User(Integer id, Double homeLatitude, Double homeLongitude) {
		this.id = id;
		this.homeLatitude = homeLatitude;
		this.homeLongitude = homeLongitude;
	}

	public User() {
	}

	public Double getHomeLatitude() {
		return homeLatitude;
	}

	public void setHomeLatitude(Double homeLatitude) {
		this.homeLatitude = homeLatitude;
	}

	public Double getHomeLongitude() {
		return homeLongitude;
	}

	public void setHomeLongitude(Double homeLongitude) {
		this.homeLongitude = homeLongitude;
	}

	public Integer getId() {
		return id;
	}
}
