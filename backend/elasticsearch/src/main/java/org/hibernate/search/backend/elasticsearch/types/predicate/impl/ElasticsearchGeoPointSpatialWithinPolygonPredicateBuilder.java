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
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class ElasticsearchGeoPointSpatialWithinPolygonPredicateBuilder extends
		AbstractElasticsearchSingleFieldPredicateBuilder
		implements SpatialWithinPolygonPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor GEO_POLYGON_ACCESSOR = JsonAccessor.root().property( "geo_polygon" ).asObject();

	private static final String POINTS_PROPERTY_NAME = "points";

	private JsonArray pointsArray;

	ElasticsearchGeoPointSpatialWithinPolygonPredicateBuilder(String absoluteFieldPath, List<String> nestedPathHierarchy) {
		super( absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public void polygon(GeoPolygon polygon) {
		this.pointsArray = new JsonArray();
		for ( GeoPoint point : polygon.points() ) {
			JsonArray pointArray = new JsonArray();
			pointArray.add( point.longitude() );
			pointArray.add( point.latitude() );
			pointsArray.add( pointArray );
		}
	}

	@Override
	protected JsonObject doBuild(
			ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		JsonObject pointsObject = new JsonObject();
		pointsObject.add( POINTS_PROPERTY_NAME, pointsArray );

		innerObject.add( absoluteFieldPath, pointsObject );
		GEO_POLYGON_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}
}
