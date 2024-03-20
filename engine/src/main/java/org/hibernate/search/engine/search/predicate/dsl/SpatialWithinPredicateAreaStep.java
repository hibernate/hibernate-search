/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

/**
 * The step in a "within" predicate definition where the area to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface SpatialWithinPredicateAreaStep<N extends SpatialWithinPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to point to a location within the given circle,
	 * i.e. a location that is at most at the given distance from the given center.
	 *
	 * @param center The center of the bounding circle.
	 * @param radius The radius of the bounding circle, in the unit defined by parameter {@code unit}.
	 * @param unit The unit used for the radius.
	 * @return The next step.
	 */
	N circle(GeoPoint center, double radius, DistanceUnit unit);

	/**
	 * Require at least one of the targeted fields to point to a location within the given circle,
	 * i.e. a location that is at most at the given distance from the given center.
	 *
	 * @param center The center of the bounding circle.
	 * @param radiusInMeters The radius of the bounding circle, in meters.
	 * @return The next step.
	 */
	default N circle(GeoPoint center, double radiusInMeters) {
		return circle( center, radiusInMeters, DistanceUnit.METERS );
	}

	/**
	 * Require at least one of the targeted fields to point to a location within the given circle,
	 * i.e. a location that is at most at the given distance from the given center.
	 *
	 * @param latitude The latitude of the center of the bounding circle.
	 * @param longitude The longitude of the center of the bounding circle.
	 * @param radius The radius of the bounding circle, in the unit defined by parameter {@code unit}.
	 * @param unit The unit used for the radius.
	 * @return The next step.
	 */
	default N circle(double latitude, double longitude, double radius, DistanceUnit unit) {
		return circle( GeoPoint.of( latitude, longitude ), radius, unit );
	}

	/**
	 * Require at least one of the targeted fields to point to a location within the given circle,
	 * i.e. a location that is at most at the given distance from the given center.
	 *
	 * @param latitude The latitude of the center of the bounding circle.
	 * @param longitude The longitude of the center of the bounding circle.
	 * @param radiusInMeters The radius of the bounding circle, in meters.
	 * @return The next step.
	 */
	default N circle(double latitude, double longitude, double radiusInMeters) {
		return circle( GeoPoint.of( latitude, longitude ), radiusInMeters, DistanceUnit.METERS );
	}

	/**
	 * Require at least one of the targeted fields to point to a location within the given polygon.
	 *
	 * @param polygon The bounding polygon.
	 * @return The next step.
	 */
	N polygon(GeoPolygon polygon);

	/**
	 * Require at least one of the targeted fields to point to a location within the given box (~rectangle).
	 *
	 * @param boundingBox The bounding box.
	 * @return The next step.
	 */
	N boundingBox(GeoBoundingBox boundingBox);

	/**
	 * Require at least one of the targeted fields to point to a location within the given box (~rectangle).
	 *
	 * @param topLeftLatitude The latitude of the top-left corner of the box.
	 * @param topLeftLongitude The longitude of the top-left corner of the box.
	 * @param bottomRightLatitude The latitude of the bottom-right corner of the box.
	 * @param bottomRightLongitude The longitude of the bottom-right corner of the box.
	 * @return The next step.
	 */
	default N boundingBox(double topLeftLatitude, double topLeftLongitude,
			double bottomRightLatitude, double bottomRightLongitude) {
		return boundingBox( GeoBoundingBox.of(
				topLeftLatitude, topLeftLongitude, bottomRightLatitude, bottomRightLongitude
		) );
	}

	/**
	 * Require at least one of the targeted fields to point to a location within the polygon that will be passed to a query via a query parameter.
	 *
	 * @param parameterName The name of a query parameter representing the bounding polygon.
	 * @return The next step.
	 */
	N polygonParam(String parameterName);

	/**
	 * Require at least one of the targeted fields to point to a location within the box (~rectangle) that will be passed to a query via a query parameter.
	 *
	 * @param parameterName The name of a query parameter representing the bounding box.
	 * @return The next step.
	 */
	N boundingBoxParam(String parameterName);

	/**
	 * Require at least one of the targeted fields to point to a location within the given circle,
	 * i.e. a location that is at most at the distance from the center, passed to a query via a query parameter.
	 *
	 * @param centerParameterName The name of a query parameter representing the center of the bounding circle.
	 * @param radiusParameterName The name of a query parameter representing the radius of the bounding circle, in the unit defined by parameter {@code unit}.
	 * @param unitParameterName The name of a query parameter representing the unit used for the radius.
	 * @return The next step.
	 */
	N circleParam(String centerParameterName, String radiusParameterName, String unitParameterName);
}
