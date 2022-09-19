/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.spatial;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Spatial;

@Spatial
@Spatial(name = "work")
@Entity
@Indexed
public class UserEx {

	@Id
	Integer id;

	@Latitude
	Double homeLatitude;

	@Longitude
	Double homeLongitude;

	@Latitude(of = "work")
	Double workLatitude;

	@Longitude(of = "work")
	Double workLongitude;

	public UserEx(Integer id, Double homeLatitude, Double homeLongitude, Double workLatitude, Double workLongitude) {
		this.id = id;
		this.homeLatitude = homeLatitude;
		this.homeLongitude = homeLongitude;
		this.workLatitude = workLatitude;
		this.workLongitude = workLongitude;
	}

	public UserEx() {
	}

	public Double getHomeLatitude() {
		return homeLatitude;
	}

	public void setHomeLatitude(Double homeLatitude) {
		this.homeLatitude = homeLatitude;
	}

	public Double getHomeLongitude() {
		return homeLongitude;
	}

	public void setHomeLongitude(Double homeLongitude) {
		this.homeLongitude = homeLongitude;
	}

	public Double getWorkLatitude() {
		return workLatitude;
	}

	public void setWorkLatitude(Double workLatitude) {
		this.workLatitude = workLatitude;
	}

	public Double getWorkLongitude() {
		return workLongitude;
	}

	public void setWorkLongitude(Double workLongitude) {
		this.workLongitude = workLongitude;
	}

	public Integer getId() {
		return id;
	}
}
