/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.spatial;

import org.hibernate.search.util.common.impl.Contracts;

/**
 * Distance units.
 */
public enum DistanceUnit {

	METERS( 1 ),

	KILOMETERS( 1000 ),

	MILES( 1_609.344 ),

	YARDS( 0.9144 ),

	NAUTICAL_MILES( 1_852 );

	private final double toMeters;

	DistanceUnit(double toMeters) {
		this.toMeters = toMeters;
	}

	public double toMeters(double distance) {
		Contracts.assertNotNull( distance, "distance" );

		return distance * toMeters;
	}

	public Double fromMeters(Double distanceInMeters) {
		if ( distanceInMeters == null ) {
			return null;
		}

		return distanceInMeters / toMeters;
	}
}
