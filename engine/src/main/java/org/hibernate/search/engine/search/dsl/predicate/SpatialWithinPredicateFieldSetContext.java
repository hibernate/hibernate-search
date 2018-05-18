/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.backend.spatial.DistanceUnit;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.engine.backend.spatial.GeoPolygon;
import org.hibernate.search.engine.backend.spatial.ImmutableGeoPoint;
import org.hibernate.search.util.impl.common.Contracts;

/**
 * The context used when defining a spatial predicate, after at least one field was mentioned.
 *
 * @param <N> The type of the next context.
 */
public interface SpatialWithinPredicateFieldSetContext<N> extends MultiFieldPredicateFieldSetContext<SpatialWithinPredicateFieldSetContext<N>> {

	SpatialWithinCirclePredicateContext<N> circle(GeoPoint center, double radiusInMeters);

	default SpatialWithinCirclePredicateContext<N> circle(double latitude, double longitude, double radiusInMeters) {
		return circle( new ImmutableGeoPoint( latitude, longitude ), radiusInMeters );
	}

	default SpatialWithinCirclePredicateContext<N> circle(GeoPoint center, double radius, DistanceUnit unit) {
		Contracts.assertNotNull( radius, "radius" );
		Contracts.assertNotNull( unit, "unit" );

		return circle( center, unit.toMeters( radius ) );
	}

	default SpatialWithinCirclePredicateContext<N> circle(double latitude, double longitude, double radius, DistanceUnit unit) {
		Contracts.assertNotNull( radius, "radius" );
		Contracts.assertNotNull( unit, "unit" );

		return circle( new ImmutableGeoPoint( latitude, longitude ), unit.toMeters( radius ) );
	}

	SpatialWithinPolygonPredicateContext<N> polygon(GeoPolygon polygon);
}
