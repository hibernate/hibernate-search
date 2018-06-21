/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class GeoPointSpatialWithinPolygonPredicateBuilder extends AbstractSearchPredicateBuilder
		implements SpatialWithinPolygonPredicateBuilder<Void, ElasticsearchSearchPredicateCollector> {

	private static final JsonObjectAccessor GEO_POLYGON = JsonAccessor.root().property( "geo_polygon" ).asObject();

	private static final String POINTS = "points";

	private final String absoluteFieldPath;

	public GeoPointSpatialWithinPolygonPredicateBuilder(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public void polygon(GeoPolygon polygon) {
		getInnerObject().add( absoluteFieldPath, toPointsObject( polygon ) );
	}

	@Override
	public void contribute(Void context, ElasticsearchSearchPredicateCollector collector) {
		JsonObject outerObject = getOuterObject();
		GEO_POLYGON.set( outerObject, getInnerObject() );
		collector.collectPredicate( outerObject );
	}

	private JsonObject toPointsObject(GeoPolygon polygon) {
		JsonArray pointsArray = new JsonArray();

		for ( GeoPoint point : polygon.getPoints() ) {
			JsonArray pointArray = new JsonArray();
			pointArray.add( point.getLongitude() );
			pointArray.add( point.getLatitude() );

			pointsArray.add( pointArray );
		}

		JsonObject pointsObject = new JsonObject();
		pointsObject.add( POINTS, pointsArray );

		return pointsObject;
	}
}
