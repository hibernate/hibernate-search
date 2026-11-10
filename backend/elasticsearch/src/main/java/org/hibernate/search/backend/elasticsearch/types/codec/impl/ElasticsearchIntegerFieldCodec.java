package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.engine.cfg.spi.NumberUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public final class ElasticsearchIntegerFieldCodec extends AbstractElasticsearchFieldCodec<Integer> {

	public ElasticsearchIntegerFieldCodec(JsonCodecService json) {
		super( json );
	}

	@Override
	public JsonNode encode(Integer value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		return JsonScalarConversions.toNode( value );
	}

	@Override
	public Integer decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}
		return JsonElementTypes.INTEGER.fromElement( element );
	}

	@Override
	public Integer decode(Double value) {
		return NumberUtils.toInteger( value );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return other instanceof ElasticsearchIntegerFieldCodec;
	}
}
