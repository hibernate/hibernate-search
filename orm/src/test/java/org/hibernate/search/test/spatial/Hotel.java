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

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.SpatialMode;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.spatial.Coordinates;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Hibernate Search spatial : Point Of Interest test entity
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
@Entity
@Indexed
@Spatial(name = "hotel_location", spatialMode = SpatialMode.GRID)
public class Hotel implements Coordinates {
	@Id
	Integer id;

	@Field(store = Store.YES)
	String name;

	@Field(store = Store.YES, index = Index.YES)
	String type;

	double latitude;
	double longitude;

	public Hotel( Integer id, String name, double latitude, double longitude, String type ) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.type = type;
	}

	public Hotel() {
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
