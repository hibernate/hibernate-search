/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.Coordinates;

/**
 * @author Emmanuel Bernard
 */
public class SpatialQueryContext {
	private String coordinatesField;
	private double radiusDistance;
	private Unit distanceUnit;
	private Coordinates coordinates;

	public String getCoordinatesField() {
		return coordinatesField != null ? coordinatesField : Spatial.COORDINATES_DEFAULT_FIELD;
	}

	public void setCoordinatesField(String coordinatesField) {
		this.coordinatesField = coordinatesField;
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
