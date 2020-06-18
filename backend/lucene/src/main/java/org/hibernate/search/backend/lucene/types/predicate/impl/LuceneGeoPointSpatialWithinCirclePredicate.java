/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;

class LuceneGeoPointSpatialWithinCirclePredicate extends AbstractLuceneSingleFieldPredicate {

	private final Query query;

	private LuceneGeoPointSpatialWithinCirclePredicate(Builder builder) {
		super( builder );
		query = builder.buildQuery();
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		return query;
	}

	static class Builder extends AbstractBuilder implements SpatialWithinCirclePredicateBuilder {
		protected GeoPoint center;
		protected double radiusInMeters;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<GeoPoint> field) {
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

		private Query buildQuery() {
			return LatLonPoint.newDistanceQuery( absoluteFieldPath, center.latitude(), center.longitude(), radiusInMeters );
		}
	}
}
