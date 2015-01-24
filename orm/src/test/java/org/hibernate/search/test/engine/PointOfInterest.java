/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.CacheFromIndex;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

import static org.hibernate.search.annotations.FieldCacheType.CLASS;
import static org.hibernate.search.annotations.FieldCacheType.ID;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
@CacheFromIndex({ CLASS, ID })
public class PointOfInterest {

	@Id
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
