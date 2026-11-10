package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public final class ElasticsearchDoubleFieldCodec extends AbstractElasticsearchFieldCodec<Double> {

	public ElasticsearchDoubleFieldCodec(JsonCodecService json) {
		super( json );
	}

	@Override
	public JsonNode encode(Double value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		return JsonScalarConversions.toNode( value );
	}

	@Override
	public Double decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}
		return JsonElementTypes.DOUBLE.fromElement( element );
	}

	@Override
	public Double decode(Double value) {
		return value;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return other instanceof ElasticsearchDoubleFieldCodec;
	}
}
