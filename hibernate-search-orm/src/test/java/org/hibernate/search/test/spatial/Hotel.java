package org.hibernate.search.test.spatial;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.SpatialFieldBridge;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Hibernate Search spatial : Point Of Interest test entity
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
@Entity
@Indexed
@Spatial
public class Hotel implements Coordinates {
	@Id
	Integer id;

	@Field(store = Store.YES)
	String name;

	@Field(store = Store.YES, index = Index.YES)
	String type;

	@Field(store = Store.YES, index = Index.YES)
	@NumericField
	double latitude;
	@Field(store = Store.YES, index = Index.YES)
	@NumericField
	double longitude;

	public Hotel( Integer id, String name, double latitude, double longitude, String type ) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.type= type;
	}

	public Hotel() {
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
