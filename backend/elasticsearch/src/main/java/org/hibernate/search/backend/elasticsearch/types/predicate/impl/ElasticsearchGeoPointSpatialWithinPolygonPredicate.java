/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ElasticsearchGeoPointSpatialWithinPolygonPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor GEO_SHAPE_ACCESSOR =
			JsonAccessor.root().property( "geo_shape" ).asObject();
	private static final JsonAccessor<Boolean> IGNORE_UNMAPPED_ACCESSOR =
			JsonAccessor.root().property( "ignore_unmapped" ).asBoolean();

	private static final String COORDINATES_PROPERTY_NAME = "coordinates";
	private static final String TYPE_PROPERTY_NAME = "type";
	private static final String TYPE_PROPERTY_VALUE = "Polygon";
	private static final String SHAPE_PROPERTY_NAME = "shape";

	private final double[] coordinates;

	private ElasticsearchGeoPointSpatialWithinPolygonPredicate(Builder builder) {
		super( builder );
		coordinates = builder.coordinates;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		JsonObject geometryObject = new JsonObject();
		JsonArray pointsArray = new JsonArray();
		for ( int i = 0; i < coordinates.length; i += 2 ) {
			JsonArray point = new JsonArray();
			point.add( coordinates[i] );
			point.add( coordinates[i + 1] );
			pointsArray.add( point );
		}
		// GeoJSON Polygon has an array around the array of actual coordinates:
		// "geometry": {
		//     "type": "Polygon",
		//     "coordinates": [
		//         [
		//             [100.0, 0.0],
		//             [101.0, 0.0],
		//             [101.0, 1.0],
		//             [100.0, 1.0],
		//             [100.0, 0.0]
		//         ]
		//     ]
		// },
		JsonArray coordinatesArray = new JsonArray();
		coordinatesArray.add( pointsArray );
		geometryObject.add( COORDINATES_PROPERTY_NAME, coordinatesArray );
		// A GeoJSON shape; in our case we'll always have a Polygon:
		geometryObject.addProperty( TYPE_PROPERTY_NAME, TYPE_PROPERTY_VALUE );

		JsonObject shapeObject = new JsonObject();
		shapeObject.add( SHAPE_PROPERTY_NAME, geometryObject );
		innerObject.add( absoluteFieldPath, shapeObject );

		if ( indexNames().size() > 1 ) {
			// There are multiple target indexes; some of them may not declare the field.
			// Instruct ES to behave as if the field had no value in that case.
			IGNORE_UNMAPPED_ACCESSOR.set( innerObject, true );
		}

		GEO_SHAPE_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	public static class Factory
			extends
			AbstractElasticsearchValueFieldSearchQueryElementFactory<SpatialWithinPolygonPredicateBuilder, GeoPoint> {
		@Override
		public Builder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<GeoPoint> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements SpatialWithinPolygonPredicateBuilder {
		private double[] coordinates;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
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
