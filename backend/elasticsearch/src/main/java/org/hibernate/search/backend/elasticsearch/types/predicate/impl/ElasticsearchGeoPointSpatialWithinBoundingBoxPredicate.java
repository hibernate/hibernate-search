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
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ElasticsearchGeoPointSpatialWithinBoundingBoxPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor GEO_BOUNDING_BOX_ACCESSOR = JsonAccessor.root().property( "geo_bounding_box" ).asObject();

	private static final String TOP_LEFT_PROPERTY_NAME = "top_left";
	private static final String BOTTOM_RIGHT_PROPERTY_NAME = "bottom_right";

	private final JsonElement topLeft;
	private final JsonElement bottomRight;

	private ElasticsearchGeoPointSpatialWithinBoundingBoxPredicate(Builder builder) {
		super( builder );
		topLeft = builder.topLeft;
		bottomRight = builder.bottomRight;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		JsonObject boundingBoxObject = new JsonObject();
		boundingBoxObject.add( TOP_LEFT_PROPERTY_NAME, topLeft );
		boundingBoxObject.add( BOTTOM_RIGHT_PROPERTY_NAME, bottomRight );

		innerObject.add( absoluteFieldPath, boundingBoxObject );

		GEO_BOUNDING_BOX_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	static class Builder extends AbstractBuilder implements SpatialWithinBoundingBoxPredicateBuilder {
		private final ElasticsearchFieldCodec<GeoPoint> codec;

		private JsonElement topLeft;
		private JsonElement bottomRight;

		Builder(ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<GeoPoint> field,
				ElasticsearchFieldCodec<GeoPoint> codec) {
			super( searchContext, field );
			this.codec = codec;
		}

		@Override
		public void boundingBox(GeoBoundingBox boundingBox) {
			this.topLeft = codec.encode( boundingBox.topLeft() );
			this.bottomRight = codec.encode( boundingBox.bottomRight() );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchGeoPointSpatialWithinBoundingBoxPredicate( this );
		}
	}
}
