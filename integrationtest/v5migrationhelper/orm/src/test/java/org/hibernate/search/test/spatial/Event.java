/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.spatial;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.spatial.Coordinates;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Hibernate Search spatial : Event test entity
 *
 * @author Nicolas Helleringer
 */
@Entity
@Indexed
public class Event {
	@Id
	Integer id;

	@Field(store = Store.YES)
	String name;

	@Field(store = Store.YES, index = Index.YES)
	@Column(name = "realdate")
	Date date;

	@Field(store = Store.YES, index = Index.YES)
	double latitude;

	@Field(store = Store.YES, index = Index.YES)
	double longitude;

	@Spatial
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

	public Event( Integer id, String name, double latitude, double longitude, Date date ) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.date = date;
	}

	public Event() {
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

	public Date getDate() {
		return date;
	}

}
