/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
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
			extends AbstractLuceneValueFieldSearchQueryElementFactory<SpatialWithinCirclePredicateBuilder, GeoPoint> {
		@Override
		public Builder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder<GeoPoint> implements SpatialWithinCirclePredicateBuilder {
		protected GeoPoint center;
		protected double radiusInMeters;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
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
