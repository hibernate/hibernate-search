/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.Coordinates;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SpatialQueryContext {
	private String coordinatesField;
	private double radiusDistance;
	private Unit distanceUnit;
	private Coordinates coordinates;

	public String getCoordinatesField() {
		return coordinatesField;
	}

	public void setCoordinatesField(String coordinatesField) {
		this.coordinatesField = coordinatesField;
	}

	public void setDefaultCoordinatesField() {
		this.coordinatesField = Spatial.COORDINATES_DEFAULT_FIELD;
	}

	public double getRadiusDistance() {
		return radiusDistance;
	}

	public Unit getDistanceUnit() {
		return distanceUnit;
	}

	public Coordinates getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}

	public void setRadius(double distance, Unit unit) {
		this.radiusDistance = distance;
		this.distanceUnit = unit;
	}
}
