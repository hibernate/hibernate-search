/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementType;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.engine.backend.spatial.ImmutableGeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class GeoPointFieldCodec implements ElasticsearchFieldCodec {
	// Must be a singleton so that equals() works as required by the interface
	public static final GeoPointFieldCodec INSTANCE = new GeoPointFieldCodec();

	private static final JsonAccessor<Double> LATITUDE_ACCESSOR =
			JsonAccessor.root().property( "lat" ).asDouble();
	private static final JsonAccessor<Double> LONGITUDE_ACCESSOR =
			JsonAccessor.root().property( "lon" ).asDouble();

	private GeoPointFieldCodec() {
	}

	@Override
	public JsonElement encode(Object object) {
		if ( object == null ) {
			return JsonNull.INSTANCE;
		}
		GeoPoint value = (GeoPoint) object;
		JsonObject result = new JsonObject();
		LATITUDE_ACCESSOR.set( result, value.getLatitude() );
		LONGITUDE_ACCESSOR.set( result, value.getLongitude() );
		return result;
	}

	@Override
	public Object decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		JsonObject object = JsonElementType.OBJECT.fromElement( element );
		double latitude = LATITUDE_ACCESSOR.get( object ).get();
		double longitude = LONGITUDE_ACCESSOR.get( object ).get();
		return new ImmutableGeoPoint( latitude, longitude );
	}
}
