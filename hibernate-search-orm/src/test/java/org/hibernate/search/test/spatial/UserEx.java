package org.hibernate.search.test.spatial;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.SpatialMode;
import org.hibernate.search.annotations.Spatials;

@Spatials({
	@Spatial,
	@Spatial(name="work",  spatialMode = SpatialMode.GRID)
		})
@Entity
@Indexed
public class UserEx {

	@Id
	Integer id;

	@Latitude
	Double homeLatitude;

	@Longitude
	Double homeLongitude;

	@Latitude(spatialName="work")
	Double workLatitude;

	@Longitude(spatialName="work")
	Double workLongitude;

	public UserEx( Integer id, Double homeLatitude, Double homeLongitude, Double workLatitude, Double workLongitude ) {
		this.id = id;
		this.homeLatitude = homeLatitude;
		this.homeLongitude = homeLongitude;
		this.workLatitude = workLatitude;
		this.workLongitude = workLongitude;
	}

	public Double getHomeLatitude() {
		return homeLatitude;
	}

	public void setHomeLatitude( Double homeLatitude ) {
		this.homeLatitude = homeLatitude;
	}

	public Double getHomeLongitude() {
		return homeLongitude;
	}

	public void setHomeLongitude( Double homeLongitude ) {
		this.homeLongitude = homeLongitude;
	}

	public Double getWorkLatitude() {
		return workLatitude;
	}

	public void setWorkLatitude( Double workLatitude ) {
		this.workLatitude = workLatitude;
	}

	public Double getWorkLongitude() {
		return workLongitude;
	}

	public void setWorkLongitude( Double workLongitude ) {
		this.workLongitude = workLongitude;
	}

	public Integer getId() {
		return id;
	}
}
