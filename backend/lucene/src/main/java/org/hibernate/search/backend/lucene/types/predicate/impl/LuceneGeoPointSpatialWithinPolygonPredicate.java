/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.search.Query;

public class LuceneGeoPointSpatialWithinPolygonPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneGeoPointSpatialWithinPolygonPredicate(Builder builder) {
		super( builder );
	}

	public static class Factory
			extends AbstractLuceneValueFieldSearchQueryElementFactory<SpatialWithinPolygonPredicateBuilder, GeoPoint> {
		@Override
		public Builder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			return new Builder( scope, field );
		}
	}

	static class Builder extends AbstractBuilder<GeoPoint> implements SpatialWithinPolygonPredicateBuilder {
		protected GeoPolygon polygon;

		Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
		}

		@Override
		public void polygon(GeoPolygon polygon) {
			this.polygon = polygon;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneGeoPointSpatialWithinPolygonPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			List<GeoPoint> points = polygon.points();

			double[] polyLats = new double[points.size()];
			double[] polyLons = new double[points.size()];

			for ( int i = 0; i < points.size(); i++ ) {
				polyLats[i] = points.get( i ).latitude();
				polyLons[i] = points.get( i ).longitude();
			}

			Polygon lucenePolygon = new Polygon( polyLats, polyLons );

			return LatLonPoint.newPolygonQuery( absoluteFieldPath, lucenePolygon );
		}
	}
}
