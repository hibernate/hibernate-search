package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public final class ElasticsearchBooleanFieldCodec extends AbstractElasticsearchFieldCodec<Boolean> {

	public ElasticsearchBooleanFieldCodec(JsonCodecService json) {
		super( json );
	}

	@Override
	public JsonNode encode(Boolean value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		return JsonScalarConversions.toNode( value );
	}

	@Override
	public Boolean decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}
		return JsonElementTypes.BOOLEAN.fromElement( element );
	}

	@Override
	public Boolean decode(Double value) {
		return value != 0;
	}

	@Override
	public Boolean decodeAggregationKey(JsonNode key, JsonNode keyAsString) {
		if ( key == null || key.isNull() ) {
			return null;
		}
		int intValue = JsonElementTypes.INTEGER.fromElement( key );
		return intValue != 0;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return other instanceof ElasticsearchBooleanFieldCodec;
	}

	@Override
	public String fromJsonElementToString(JsonNode value) {
		if ( value == null || value.isNull() ) {
			return null;
		}
		// Sometimes the backend may return boolean as `true` and sometimes as `"true"`,
		//  hence we just get the boolean value and convert it to String as:
		return Boolean.toString( JsonScalarConversions.asBoolean( value ) );
	}
}
