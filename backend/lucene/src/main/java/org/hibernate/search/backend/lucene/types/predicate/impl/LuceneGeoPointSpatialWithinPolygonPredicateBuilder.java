/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.search.Query;

class LuceneGeoPointSpatialWithinPolygonPredicateBuilder extends AbstractLuceneSpatialWithinPolygonPredicateBuilder {

	LuceneGeoPointSpatialWithinPolygonPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<GeoPoint> field) {
		super( searchContext, field );
	}

	@Override
	protected Query doBuild(PredicateRequestContext context) {
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
