/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.spatial.geopointbinding.multiple;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

//tag::include[]
@Entity
@Indexed
@GeoPointBinding(fieldName = "placeOfBirth", markerSet = "birth") // <1>
@GeoPointBinding(fieldName = "placeOfDeath", markerSet = "death") // <2>
public class Author {

	@Id
	@GeneratedValue
	private Integer id;

	@FullTextField(analyzer = "name")
	private String name;

	@Latitude(markerSet = "birth") // <3>
	private Double placeOfBirthLatitude;

	@Longitude(markerSet = "birth") // <4>
	private Double placeOfBirthLongitude;

	@Latitude(markerSet = "death") // <5>
	private Double placeOfDeathLatitude;

	@Longitude(markerSet = "death") // <6>
	private Double placeOfDeathLongitude;

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

	public Double getPlaceOfDeathLatitude() {
		return placeOfDeathLatitude;
	}

	public void setPlaceOfDeathLatitude(Double placeOfDeathLatitude) {
		this.placeOfDeathLatitude = placeOfDeathLatitude;
	}

	public Double getPlaceOfDeathLongitude() {
		return placeOfDeathLongitude;
	}

	public void setPlaceOfDeathLongitude(Double placeOfDeathLongitude) {
		this.placeOfDeathLongitude = placeOfDeathLongitude;
	}
	//end::getters-setters[]
}
//end::include[]
