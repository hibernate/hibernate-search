/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.predicate;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

import org.hibernate.search.engine.spatial.GeoPoint;

@Embeddable
public class EmbeddableGeoPoint implements GeoPoint {

	public static EmbeddableGeoPoint of(double latitude, double longitude) {
		return new EmbeddableGeoPoint( latitude, longitude );
	}

	@Basic
	private Double latitude;
	@Basic
	private Double longitude;

	protected EmbeddableGeoPoint() {
		// For Hibernate ORM
	}

	private EmbeddableGeoPoint(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	@Basic
	public double latitude() {
		return latitude;
	}

	@Override
	@Basic
	public double longitude() {
		return longitude;
	}
}
