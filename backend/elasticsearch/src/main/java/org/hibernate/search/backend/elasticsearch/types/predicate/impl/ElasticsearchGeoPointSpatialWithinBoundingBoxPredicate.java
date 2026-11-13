package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchGeoPointSpatialWithinBoundingBoxPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor GEO_BOUNDING_BOX_ACCESSOR =
			JsonAccessor.root().property( "geo_bounding_box" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<Boolean> IGNORE_UNMAPPED_ACCESSOR =
			JsonAccessor.root().property( "ignore_unmapped" ).unwrapSingletonArrays().asBoolean();

	private static final String TOP_LEFT_PROPERTY_NAME = "top_left";

	private static final String BOTTOM_RIGHT_PROPERTY_NAME = "bottom_right";

	private final JsonNode topLeft;

	private final JsonNode bottomRight;

	private ElasticsearchGeoPointSpatialWithinBoundingBoxPredicate(Builder builder) {
		super( builder );
		topLeft = builder.topLeft;
		bottomRight = builder.bottomRight;
	}

	@Override
	protected ObjectNode doToJsonQuery(PredicateRequestContext context, ObjectNode outerObject, ObjectNode innerObject) {
		ObjectNode boundingBoxObject = new ObjectNode( JsonNodeFactory.instance );
		boundingBoxObject.set( TOP_LEFT_PROPERTY_NAME, topLeft );
		boundingBoxObject.set( BOTTOM_RIGHT_PROPERTY_NAME, bottomRight );
		innerObject.set( absoluteFieldPath, boundingBoxObject );
		if ( indexNames().size() > 1 ) {
			// There are multiple target indexes; some of them may not declare the field.
			// Instruct ES to behave as if the field had no value in that case.
			IGNORE_UNMAPPED_ACCESSOR.set( innerObject, true );
		}
		GEO_BOUNDING_BOX_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	public static class Factory
			extends
			AbstractElasticsearchCodecAwareSearchQueryElementFactory<SpatialWithinBoundingBoxPredicateBuilder, GeoPoint> {

		public Factory(ElasticsearchFieldCodec<GeoPoint> codec) {
			super( codec );
		}

		@Override
		public Builder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<GeoPoint> field) {
			return new Builder( codec, scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements SpatialWithinBoundingBoxPredicateBuilder {

		private final ElasticsearchFieldCodec<GeoPoint> codec;

		private JsonNode topLeft;

		private JsonNode bottomRight;

		private Builder(ElasticsearchFieldCodec<GeoPoint> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
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
