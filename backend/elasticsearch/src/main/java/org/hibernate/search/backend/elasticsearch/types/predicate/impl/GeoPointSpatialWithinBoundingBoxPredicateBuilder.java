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
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.spatial.GeoBoundingBox;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;

import com.google.gson.JsonObject;

class GeoPointSpatialWithinBoundingBoxPredicateBuilder extends AbstractSearchPredicateBuilder
		implements SpatialWithinBoundingBoxPredicateBuilder<ElasticsearchSearchPredicateCollector> {

	private static final JsonObjectAccessor GEO_BOUNDING_BOX = JsonAccessor.root().property( "geo_bounding_box" ).asObject();

	private static final String TOP_LEFT = "top_left";

	private static final String BOTTOM_RIGHT = "bottom_right";

	private final String absoluteFieldPath;

	private final ElasticsearchFieldCodec codec;

	public GeoPointSpatialWithinBoundingBoxPredicateBuilder(String absoluteFieldPath, ElasticsearchFieldCodec codec) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
	}

	@Override
	public void boundingBox(GeoBoundingBox boundingBox) {
		getInnerObject().add( absoluteFieldPath, toBoundingBoxObject( boundingBox ) );
	}

	@Override
	public void contribute(ElasticsearchSearchPredicateCollector collector) {
		JsonObject outerObject = getOuterObject();
		GEO_BOUNDING_BOX.set( outerObject, getInnerObject() );
		collector.collectPredicate( outerObject );
	}

	private JsonObject toBoundingBoxObject(GeoBoundingBox boundingBox) {
		JsonObject boundingBoxObject = new JsonObject();

		boundingBoxObject.add( TOP_LEFT, codec.encode( boundingBox.getTopLeft() ) );
		boundingBoxObject.add( BOTTOM_RIGHT, codec.encode( boundingBox.getBottomRight() ) );

		return boundingBoxObject;
	}
}
