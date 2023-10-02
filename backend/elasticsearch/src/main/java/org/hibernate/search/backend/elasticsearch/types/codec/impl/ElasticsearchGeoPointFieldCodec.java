/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class ElasticsearchGeoPointFieldCodec implements ElasticsearchFieldCodec<GeoPoint> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Must be a singleton so that equals() works as required by the interface
	public static final ElasticsearchGeoPointFieldCodec INSTANCE = new ElasticsearchGeoPointFieldCodec();

	private static final JsonAccessor<Double> LATITUDE_ACCESSOR =
			JsonAccessor.root().property( "lat" ).asDouble();
	private static final JsonAccessor<Double> LONGITUDE_ACCESSOR =
			JsonAccessor.root().property( "lon" ).asDouble();

	private ElasticsearchGeoPointFieldCodec() {
	}

	@Override
	public JsonElement encode(GeoPoint value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		JsonObject result = new JsonObject();
		LATITUDE_ACCESSOR.set( result, value.latitude() );
		LONGITUDE_ACCESSOR.set( result, value.longitude() );
		return result;
	}

	@Override
	public GeoPoint decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		JsonObject object = JsonElementTypes.OBJECT.fromElement( element );
		double latitude = LATITUDE_ACCESSOR.get( object ).orElseThrow( log::elasticsearchResponseMissingData );
		double longitude = LONGITUDE_ACCESSOR.get( object ).orElseThrow( log::elasticsearchResponseMissingData );
		return GeoPoint.of( latitude, longitude );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return INSTANCE == other;
	}
}
