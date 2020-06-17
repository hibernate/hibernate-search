/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ElasticsearchGeoPointSpatialWithinCirclePredicateBuilder
		extends AbstractElasticsearchSingleFieldPredicateBuilder
		implements SpatialWithinCirclePredicateBuilder {

	private static final JsonObjectAccessor GEO_DISTANCE_ACCESSOR = JsonAccessor.root().property( "geo_distance" ).asObject();

	private static final JsonAccessor<Double> DISTANCE_ACCESSOR = JsonAccessor.root().property( "distance" ).asDouble();

	private final ElasticsearchFieldCodec<GeoPoint> codec;

	private double distanceInMeters;
	private JsonElement center;

	ElasticsearchGeoPointSpatialWithinCirclePredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<GeoPoint> field, ElasticsearchFieldCodec<GeoPoint> codec) {
		super( searchContext, field );
		this.codec = codec;
	}

	@Override
	public void circle(GeoPoint center, double radius, DistanceUnit unit) {
		this.distanceInMeters = unit.toMeters( radius );
		this.center = codec.encode( center );
	}

	@Override
	protected JsonObject doBuild(
			PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		DISTANCE_ACCESSOR.set( innerObject, distanceInMeters );
		innerObject.add( absoluteFieldPath, center );

		GEO_DISTANCE_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

}
