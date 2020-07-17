/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneSearchFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;

public class LuceneGeoPointSpatialWithinCirclePredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneGeoPointSpatialWithinCirclePredicate(Builder builder) {
		super( builder );
	}

	public static class Factory
			extends AbstractLuceneSearchFieldQueryElementFactory<SpatialWithinCirclePredicateBuilder, GeoPoint, LuceneFieldCodec<GeoPoint>> {
		public Factory(LuceneFieldCodec<GeoPoint> codec) {
			super( codec );
		}

		@Override
		public Builder create(LuceneSearchContext searchContext, LuceneSearchFieldContext<GeoPoint> field) {
			return new Builder( searchContext, field );
		}
	}

	private static class Builder extends AbstractBuilder<GeoPoint> implements SpatialWithinCirclePredicateBuilder {
		protected GeoPoint center;
		protected double radiusInMeters;

		private Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<GeoPoint> field) {
			super( searchContext, field );
		}

		@Override
		public void circle(GeoPoint center, double radius, DistanceUnit unit) {
			this.center = center;
			this.radiusInMeters = unit.toMeters( radius );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneGeoPointSpatialWithinCirclePredicate( this );
		}

		@Override
		protected Query buildQuery() {
			return LatLonPoint.newDistanceQuery( absoluteFieldPath, center.latitude(), center.longitude(), radiusInMeters );
		}
	}
}
