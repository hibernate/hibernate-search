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
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonObject;

class GeoPointSpatialWithinCirclePredicateBuilder extends AbstractSearchPredicateBuilder
		implements SpatialWithinCirclePredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor GEO_DISTANCE = JsonAccessor.root().property( "geo_distance" ).asObject();

	private static final JsonAccessor<Double> DISTANCE = JsonAccessor.root().property( "distance" ).asDouble();

	private final String absoluteFieldPath;

	private final ElasticsearchFieldCodec codec;

	GeoPointSpatialWithinCirclePredicateBuilder(String absoluteFieldPath, ElasticsearchFieldCodec codec) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
	}

	@Override
	public void circle(GeoPoint center, double radius, DistanceUnit unit) {
		DISTANCE.set( getInnerObject(), unit.toMeters( radius ) );
		getInnerObject().add( absoluteFieldPath, codec.encode( center ) );
	}

	@Override
	protected JsonObject doBuild() {
		JsonObject outerObject = getOuterObject();
		GEO_DISTANCE.set( outerObject, getInnerObject() );
		return outerObject;
	}

}
