/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchClientLog;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public final class ElasticsearchGeoPointFieldCodec extends AbstractElasticsearchFieldCodec<GeoPoint> {

	private static final JsonAccessor<Double> LATITUDE_ACCESSOR =
			JsonAccessor.root().property( "lat" ).asDouble();
	private static final JsonAccessor<Double> LONGITUDE_ACCESSOR =
			JsonAccessor.root().property( "lon" ).asDouble();

	public ElasticsearchGeoPointFieldCodec(Gson gson) {
		super( gson );
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
		double latitude =
				LATITUDE_ACCESSOR.get( object )
						.orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );
		double longitude =
				LONGITUDE_ACCESSOR.get( object )
						.orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );
		return GeoPoint.of( latitude, longitude );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return other instanceof ElasticsearchGeoPointFieldCodec;
	}
}
