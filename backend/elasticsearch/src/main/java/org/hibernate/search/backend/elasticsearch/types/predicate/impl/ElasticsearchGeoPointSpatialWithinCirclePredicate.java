/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.AbstractElasticsearchCodecAwareSearchFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchValueFieldContext;
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

	private static final JsonObjectAccessor GEO_DISTANCE_ACCESSOR = JsonAccessor.root().property( "geo_distance" ).asObject();

	private static final JsonAccessor<Double> DISTANCE_ACCESSOR = JsonAccessor.root().property( "distance" ).asDouble();

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

		GEO_DISTANCE_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	public static class Factory
			extends AbstractElasticsearchCodecAwareSearchFieldQueryElementFactory<SpatialWithinCirclePredicateBuilder, GeoPoint> {
		public Factory(ElasticsearchFieldCodec<GeoPoint> codec) {
			super( codec );
		}

		@Override
		public Builder create(ElasticsearchSearchContext searchContext, ElasticsearchSearchValueFieldContext<GeoPoint> field) {
			return new Builder( codec, searchContext, field );
		}
	}

	private static class Builder extends AbstractBuilder implements SpatialWithinCirclePredicateBuilder {
		private final ElasticsearchFieldCodec<GeoPoint> codec;

		private double distanceInMeters;
		private JsonElement center;

		private Builder(ElasticsearchFieldCodec<GeoPoint> codec, ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<GeoPoint> field) {
			super( searchContext, field );
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
