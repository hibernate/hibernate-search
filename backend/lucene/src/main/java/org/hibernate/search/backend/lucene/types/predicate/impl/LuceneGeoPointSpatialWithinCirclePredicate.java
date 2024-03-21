/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.distanceInMeters;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;

public class LuceneGeoPointSpatialWithinCirclePredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneGeoPointSpatialWithinCirclePredicate(Builder builder) {
		super( builder );
	}

	public static class Factory
			extends AbstractLuceneValueFieldSearchQueryElementFactory<SpatialWithinCirclePredicateBuilder, GeoPoint> {
		@Override
		public Builder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder<GeoPoint> implements SpatialWithinCirclePredicateBuilder {
		protected QueryParametersValueProvider<GeoPoint> centerProvider;
		protected QueryParametersValueProvider<Double> radiusInMetersProvider;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
		}

		@Override
		public void circle(GeoPoint center, double radius, DistanceUnit unit) {
			this.centerProvider = simple( center );
			this.radiusInMetersProvider = simple( unit.toMeters( radius ) );
		}

		@Override
		public void param(String center, String radius, String unit) {
			this.centerProvider = parameter( center, GeoPoint.class );
			this.radiusInMetersProvider = distanceInMeters( radius, unit );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneGeoPointSpatialWithinCirclePredicate( this );
		}

		@Override
		protected Query buildQuery(PredicateRequestContext context) {
			GeoPoint center = centerProvider.provide( context.toQueryParametersContext() );
			Double radiusInMeters = radiusInMetersProvider.provide( context.toQueryParametersContext() );

			return LatLonPoint.newDistanceQuery( absoluteFieldPath, center.latitude(), center.longitude(), radiusInMeters );
		}
	}
}
