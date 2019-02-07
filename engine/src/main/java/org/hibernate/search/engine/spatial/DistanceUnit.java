/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spatial;

import org.hibernate.search.util.impl.common.Contracts;

/**
 * Distance units.
 */
public enum DistanceUnit {

	METERS(1),

	KILOMETERS(1000),

	MILES(1_609.344),

	YARDS(0.9144),

	NAUTICAL_MILES(1_852);

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
