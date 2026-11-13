package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public final class ElasticsearchStringFieldCodec extends AbstractElasticsearchFieldCodec<String> {

	public ElasticsearchStringFieldCodec(JsonCodecService json) {
		super( json );
	}

	@Override
	public JsonNode encode(String value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		return JsonScalarConversions.toNode( value );
	}

	@Override
	public String decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}
		return JsonElementTypes.STRING.fromElement( element );
	}

	@Override
	public String decode(Double value) {
		return value.toString();
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return other instanceof ElasticsearchStringFieldCodec;
	}
}
