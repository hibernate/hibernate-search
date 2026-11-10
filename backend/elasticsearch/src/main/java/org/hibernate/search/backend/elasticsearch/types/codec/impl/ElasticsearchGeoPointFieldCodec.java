package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.engine.spatial.GeoPoint;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

public final class ElasticsearchGeoPointFieldCodec extends AbstractElasticsearchFieldCodec<GeoPoint> {

	private static final JsonAccessor<Double> LATITUDE_ACCESSOR =
			JsonAccessor.root().property( "lat" ).unwrapSingletonArrays().asDouble();
	private static final JsonAccessor<Double> LONGITUDE_ACCESSOR =
			JsonAccessor.root().property( "lon" ).unwrapSingletonArrays().asDouble();

	public ElasticsearchGeoPointFieldCodec(JsonCodecService json) {
		super( json );
	}

	@Override
	public JsonNode encode(GeoPoint value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		LATITUDE_ACCESSOR.set( result, value.latitude() );
		LONGITUDE_ACCESSOR.set( result, value.longitude() );
		return result;
	}

	@Override
	public GeoPoint decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}
		ObjectNode object = JsonElementTypes.OBJECT.fromElement( element );
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
