/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class ElasticsearchGeoPointSpatialWithinPolygonPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor GEO_POLYGON_ACCESSOR = JsonAccessor.root().property( "geo_polygon" ).asObject();

	private static final String POINTS_PROPERTY_NAME = "points";

	private final double[] coordinates;

	private ElasticsearchGeoPointSpatialWithinPolygonPredicate(Builder builder) {
		super( builder );
		coordinates = builder.coordinates;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		JsonObject pointsObject = new JsonObject();
		JsonArray pointsArray = new JsonArray();
		for ( int i = 0; i < coordinates.length; i += 2 ) {
			JsonArray point = new JsonArray();
			point.add( coordinates[i] );
			point.add( coordinates[i + 1] );
			pointsArray.add( point );
		}
		pointsObject.add( POINTS_PROPERTY_NAME, pointsArray );

		innerObject.add( absoluteFieldPath, pointsObject );
		GEO_POLYGON_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	static class Builder extends AbstractBuilder implements SpatialWithinPolygonPredicateBuilder {
		private double[] coordinates;

		Builder(ElasticsearchSearchContext searchContext,
				ElasticsearchSearchFieldContext<GeoPoint> field) {
			super( searchContext, field );
		}

		@Override
		public void polygon(GeoPolygon polygon) {
			List<GeoPoint> points = polygon.points();
			this.coordinates = new double[points.size() * 2];
			int index = 0;
			for ( GeoPoint point : points ) {
				coordinates[index++] = point.longitude();
				coordinates[index++] = point.latitude();
			}
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchGeoPointSpatialWithinPolygonPredicate( this );
		}
	}
}
