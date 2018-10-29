/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

/**
 * The context used when defining a spatial predicate, after at least one field was mentioned.
 *
 * @param <N> The type of the next context.
 */
public interface SpatialWithinPredicateFieldSetContext<N> extends MultiFieldPredicateFieldSetContext<SpatialWithinPredicateFieldSetContext<N>> {

	/**
	 * Target the given field in the "within" predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link SpatialWithinPredicateContext#onField(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return {@code this}, for method chaining.
	 *
	 * @see SpatialWithinPredicateContext#onField(String)
	 */
	default SpatialWithinPredicateFieldSetContext<N> orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the "within" predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link SpatialWithinPredicateContext#onFields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return {@code this}, for method chaining.
	 *
	 * @see SpatialWithinPredicateContext#onFields(String...)
	 */
	SpatialWithinPredicateFieldSetContext<N> orFields(String ... absoluteFieldPaths);

	/**
	 * Require at least one of the targeted fields to point to a location within the given circle,
	 * i.e. a location that is at most at the given distance from the given center.
	 *
	 * @param center The center of the bounding circle.
	 * @param radius The radius of the bounding circle, in the unit defined by parameter {@code unit}.
	 * @param unit The unit used for the radius.
	 * @return A context allowing to end the predicate definition.
	 */
	SearchPredicateTerminalContext<N> circle(GeoPoint center, double radius, DistanceUnit unit);

	/**
	 * Require at least one of the targeted fields to point to a location within the given circle,
	 * i.e. a location that is at most at the given distance from the given center.
	 *
	 * @param center The center of the bounding circle.
	 * @param radiusInMeters The radius of the bounding circle, in meters.
	 * @return A context allowing to end the predicate definition.
	 */
	default SearchPredicateTerminalContext<N> circle(GeoPoint center, double radiusInMeters) {
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
	 * @return A context allowing to end the predicate definition.
	 */
	default SearchPredicateTerminalContext<N> circle(double latitude, double longitude, double radius, DistanceUnit unit) {
		return circle( GeoPoint.of( latitude, longitude ), radius, unit );
	}

	/**
	 * Require at least one of the targeted fields to point to a location within the given circle,
	 * i.e. a location that is at most at the given distance from the given center.
	 *
	 * @param latitude The latitude of the center of the bounding circle.
	 * @param longitude The longitude of the center of the bounding circle.
	 * @param radiusInMeters The radius of the bounding circle, in meters.
	 * @return A context allowing to end the predicate definition.
	 */
	default SearchPredicateTerminalContext<N> circle(double latitude, double longitude, double radiusInMeters) {
		return circle( GeoPoint.of( latitude, longitude ), radiusInMeters, DistanceUnit.METERS );
	}

	/**
	 * Require at least one of the targeted fields to point to a location within the given polygon.
	 *
	 * @param polygon The bounding polygon.
	 * @return A context allowing to end the predicate definition.
	 */
	SearchPredicateTerminalContext<N> polygon(GeoPolygon polygon);

	/**
	 * Require at least one of the targeted fields to point to a location within the given box (~rectangle).
	 *
	 * @param boundingBox The bounding box.
	 * @return A context allowing to end the predicate definition.
	 */
	SearchPredicateTerminalContext<N> boundingBox(GeoBoundingBox boundingBox);

	/**
	 * Require at least one of the targeted fields to point to a location within the given box (~rectangle).
	 *
	 * @param topLeftLatitude The latitude of the top-left corner of the box.
	 * @param topLeftLongitude The longitude of the top-left corner of the box.
	 * @param bottomRightLatitude The latitude of the bottom-right corner of the box.
	 * @param bottomRightLongitude The longitude of the bottom-right corner of the box.
	 * @return A context allowing to end the predicate definition.
	 */
	default SearchPredicateTerminalContext<N> boundingBox(double topLeftLatitude, double topLeftLongitude, double bottomRightLatitude,
			double bottomRightLongitude) {
		return boundingBox( GeoBoundingBox.of( topLeftLatitude, topLeftLongitude, bottomRightLatitude, bottomRightLongitude ) );
	}
}
