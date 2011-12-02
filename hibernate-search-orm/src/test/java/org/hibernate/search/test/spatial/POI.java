package org.hibernate.search.test.spatial;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.spatial.SpatialFieldBridge;
import org.hibernate.search.spatial.SpatialIndexable;
import org.hibernate.search.spatial.SpatialFieldBridge;
import org.hibernate.search.spatial.SpatialIndexable;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Indexed
public class POI {
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

	@Field(store = Store.YES, index = Index.YES, analyze = Analyze.NO)
	@FieldBridge(impl = SpatialFieldBridge.class)
	@Embedded
	public SpatialIndexable getLocation() {
		return new SpatialIndexable() {
			@Override
			public double getLatitude() {
				return latitude;
			}

			@Override
			public double getLongitude() {
				return longitude;
			}
		};
	}

	public POI(Integer id, String name, double latitude, double longitude, String type) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.type= type;
	}

	public POI() {
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
