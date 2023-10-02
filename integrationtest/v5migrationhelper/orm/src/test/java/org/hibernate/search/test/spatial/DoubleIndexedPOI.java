/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.spatial;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.spatial.Coordinates;

/**
 * Hibernate Search spatial : Double indexed Point Of Interest test entity for
 * cheching behavior between @Latitude+@Longitude and @FieldBridge
 *
 * @author Nicolas Helleringer
 */
@Entity
@Indexed
@Spatial
public class DoubleIndexedPOI {
	@Id
	@Field(name = "idSort")
	@SortableField(forField = "idSort")
	Integer id;

	@Field(store = Store.YES)
	String name;

	@Field(store = Store.YES, index = Index.YES)
	String type;

	@Latitude
	@Field(store = Store.YES, index = Index.YES)
	double latitude;

	@Longitude
	@Field(store = Store.YES, index = Index.YES)
	double longitude;

	@Spatial(store = Store.YES)
	@Embedded // Leaving this because it was there in Search 5, but it's being ignored by ORM, which considers the method transient.
	@IndexingDependency(derivedFrom = {
			@ObjectPath(@PropertyValue(propertyName = "latitude")),
			@ObjectPath(@PropertyValue(propertyName = "longitude"))
	})
	public Coordinates getLocation() {
		return new Coordinates() {
			@Override
			public Double getLatitude() {
				return latitude;
			}

			@Override
			public Double getLongitude() {
				return longitude;
			}
		};
	}

	public DoubleIndexedPOI(Integer id, String name, double latitude, double longitude, String type) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.type = type;
	}

	public DoubleIndexedPOI() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getType() {
		return type;
	}

}
