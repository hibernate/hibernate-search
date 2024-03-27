/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.spatial.geopointbinding.type;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

//tag::include[]
@Entity
@Indexed
@GeoPointBinding(fieldName = "placeOfBirth") // <1>
public class Author {

	@Id
	@GeneratedValue
	private Integer id;

	private String name;

	@Latitude // <2>
	private Double placeOfBirthLatitude;

	@Longitude // <3>
	private Double placeOfBirthLongitude;

	public Author() {
	}

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getPlaceOfBirthLatitude() {
		return placeOfBirthLatitude;
	}

	public void setPlaceOfBirthLatitude(Double placeOfBirthLatitude) {
		this.placeOfBirthLatitude = placeOfBirthLatitude;
	}

	public Double getPlaceOfBirthLongitude() {
		return placeOfBirthLongitude;
	}

	public void setPlaceOfBirthLongitude(Double placeOfBirthLongitude) {
		this.placeOfBirthLongitude = placeOfBirthLongitude;
	}
	//end::getters-setters[]
}
//end::include[]
