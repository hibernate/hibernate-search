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
import org.hibernate.search.spatial.Coordinates;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Hibernate Search spatial: Hotel test entity with Range Spatial indexation
 *
 * @author Nicolas Helleringer
 */
@Entity
@Indexed
@Spatial
public class RangeHotel implements Coordinates {
	@Id
	Integer id;

	@Field(store = Store.YES)
	String name;

	@Field(store = Store.YES, index = Index.YES)
	String type;

	double latitude;
	double longitude;

	public RangeHotel(Integer id, String name, double latitude, double longitude, String type) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.type = type;
	}

	public RangeHotel() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public Double getLatitude() {
		return latitude;
	}

	@Override
	public Double getLongitude() {
		return longitude;
	}

	public String getType() {
		return type;
	}

}
