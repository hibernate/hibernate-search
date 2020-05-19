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
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ElasticsearchGeoPointSpatialWithinBoundingBoxPredicateBuilder extends
		AbstractElasticsearchSingleFieldPredicateBuilder
		implements SpatialWithinBoundingBoxPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor GEO_BOUNDING_BOX_ACCESSOR = JsonAccessor.root().property( "geo_bounding_box" ).asObject();

	private static final String TOP_LEFT_PROPERTY_NAME = "top_left";

	private static final String BOTTOM_RIGHT_PROPERTY_NAME = "bottom_right";

	private final ElasticsearchFieldCodec<GeoPoint> codec;

	private JsonElement topLeft;
	private JsonElement bottomRight;

	ElasticsearchGeoPointSpatialWithinBoundingBoxPredicateBuilder(String absoluteFieldPath,
			List<String> nestedPathHierarchy, ElasticsearchFieldCodec<GeoPoint> codec) {
		super( absoluteFieldPath, nestedPathHierarchy );
		this.codec = codec;
	}

	@Override
	public void boundingBox(GeoBoundingBox boundingBox) {
		this.topLeft = codec.encode( boundingBox.topLeft() );
		this.bottomRight = codec.encode( boundingBox.bottomRight() );
	}

	@Override
	protected JsonObject doBuild(
			ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		JsonObject boundingBoxObject = new JsonObject();
		boundingBoxObject.add( TOP_LEFT_PROPERTY_NAME, topLeft );
		boundingBoxObject.add( BOTTOM_RIGHT_PROPERTY_NAME, bottomRight );

		innerObject.add( absoluteFieldPath, boundingBoxObject );

		GEO_BOUNDING_BOX_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

}
