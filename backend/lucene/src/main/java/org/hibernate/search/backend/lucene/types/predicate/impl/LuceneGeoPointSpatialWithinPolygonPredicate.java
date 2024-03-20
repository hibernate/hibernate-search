/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import java.util.List;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;
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
		protected QueryParametersValueProvider<GeoPolygon> polygonProvider;

		Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
		}

		@Override
		public void polygon(GeoPolygon polygon) {
			this.polygonProvider = simple( polygon );
		}

		@Override
		public void param(String parameterName) {
			this.polygonProvider = parameter( parameterName, GeoPolygon.class );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneGeoPointSpatialWithinPolygonPredicate( this );
		}

		@Override
		protected Query buildQuery(PredicateRequestContext context) {
			List<GeoPoint> points = polygonProvider.provide( context ).points();

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
