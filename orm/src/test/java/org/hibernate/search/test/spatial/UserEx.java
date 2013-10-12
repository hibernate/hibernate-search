/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
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
	@Spatial(name = "work", spatialMode = SpatialMode.GRID)
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
