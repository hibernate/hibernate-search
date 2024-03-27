/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchGeoPointSpatialWithinCirclePredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor GEO_DISTANCE_ACCESSOR =
			JsonAccessor.root().property( "geo_distance" ).asObject();
	private static final JsonAccessor<Double> DISTANCE_ACCESSOR =
			JsonAccessor.root().property( "distance" ).asDouble();
	private static final JsonAccessor<Boolean> IGNORE_UNMAPPED_ACCESSOR =
			JsonAccessor.root().property( "ignore_unmapped" ).asBoolean();

	private final double distanceInMeters;
	private final JsonElement center;

	private ElasticsearchGeoPointSpatialWithinCirclePredicate(Builder builder) {
		super( builder );
		distanceInMeters = builder.distanceInMeters;
		center = builder.center;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		DISTANCE_ACCESSOR.set( innerObject, distanceInMeters );
		innerObject.add( absoluteFieldPath, center );

		if ( indexNames().size() > 1 ) {
			// There are multiple target indexes; some of them may not declare the field.
			// Instruct ES to behave as if the field had no value in that case.
			IGNORE_UNMAPPED_ACCESSOR.set( innerObject, true );
		}

		GEO_DISTANCE_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	public static class Factory
			extends
			AbstractElasticsearchCodecAwareSearchQueryElementFactory<SpatialWithinCirclePredicateBuilder, GeoPoint> {
		public Factory(ElasticsearchFieldCodec<GeoPoint> codec) {
			super( codec );
		}

		@Override
		public Builder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<GeoPoint> field) {
			return new Builder( codec, scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements SpatialWithinCirclePredicateBuilder {
		private final ElasticsearchFieldCodec<GeoPoint> codec;

		private double distanceInMeters;
		private JsonElement center;

		private Builder(ElasticsearchFieldCodec<GeoPoint> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
			this.codec = codec;
		}

		@Override
		public void circle(GeoPoint center, double radius, DistanceUnit unit) {
			this.distanceInMeters = unit.toMeters( radius );
			this.center = codec.encode( center );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchGeoPointSpatialWithinCirclePredicate( this );
		}
	}
}
