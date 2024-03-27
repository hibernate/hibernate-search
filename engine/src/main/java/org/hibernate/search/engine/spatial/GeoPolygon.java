/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.spatial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A polygon in the geocentric coordinate system.
 */
public interface GeoPolygon {

	/**
	 * @return The points of this polygon.
	 * The first and last points are always identical.
	 */
	List<GeoPoint> points();

	/**
	 * Create a {@link GeoPolygon} from a list of points.
	 * <p>
	 * The first and last points must be identical.
	 *
	 * @param points The list of points. Must not be null. Must contain at least four points.
	 * @return The corresponding {@link GeoPolygon}.
	 * @throws IllegalArgumentException If the list is null, or if the first and last points are not identical.
	 */
	static GeoPolygon of(List<GeoPoint> points) {
		Contracts.assertNotNull( points, "points" );

		return new ImmutableGeoPolygon( CollectionHelper.toImmutableList( new ArrayList<>( points ) ) );
	}

	/**
	 * Create a {@link GeoPolygon} from points.
	 * <p>
	 * The first and last points must be identical.
	 *
	 * @param firstPoint The first point. Must not be null.
	 * @param secondPoint The second point. Must not be null.
	 * @param thirdPoint The third point. Must not be null.
	 * @param fourthPoint The fourth point. Must not be null.
	 * @param additionalPoints An array of additional points. Must not be null. May be empty.
	 * @return The corresponding {@link GeoPolygon}.
	 * @throws IllegalArgumentException If any of the arguments is null, or if the first and last points are not identical.
	 */
	static GeoPolygon of(GeoPoint firstPoint, GeoPoint secondPoint, GeoPoint thirdPoint, GeoPoint fourthPoint,
			GeoPoint... additionalPoints) {
		Contracts.assertNotNull( firstPoint, "firstPoint" );
		Contracts.assertNotNull( secondPoint, "secondPoint" );
		Contracts.assertNotNull( thirdPoint, "thirdPoint" );
		Contracts.assertNotNull( fourthPoint, "fourthPoint" );
		Contracts.assertNotNull( additionalPoints, "additionalPoints" );

		List<GeoPoint> points = new ArrayList<>();
		points.add( firstPoint );
		points.add( secondPoint );
		points.add( thirdPoint );
		points.add( fourthPoint );
		Collections.addAll( points, additionalPoints );

		return new ImmutableGeoPolygon( CollectionHelper.toImmutableList( points ) );
	}
}
