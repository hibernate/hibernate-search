package org.hibernate.search.test.spatial;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Hibernate Search spatial : Non Geo enabled Point Of Interest test entity
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
@Entity
@Indexed
public class NonGeoPOI {
	@Id
	Integer id;

	@Field(store = Store.YES)
	String name;

	@Field(store = Store.YES, index = Index.YES)
	String type;

	@Field(store = Store.YES, index = Index.YES)
	@NumericField
	Double latitude;
	@Field(store = Store.YES, index = Index.YES)
	@NumericField
	Double longitude;

	public NonGeoPOI(Integer id, String name, Double latitude, Double longitude, String type) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.type= type;
	}

	public NonGeoPOI() {
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
