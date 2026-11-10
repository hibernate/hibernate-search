package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.engine.cfg.spi.NumberUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public final class ElasticsearchFloatFieldCodec extends AbstractElasticsearchFieldCodec<Float> {

	public ElasticsearchFloatFieldCodec(JsonCodecService json) {
		super( json );
	}

	@Override
	public JsonNode encode(Float value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		return JsonScalarConversions.toNode( value );
	}

	@Override
	public Float decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}
		return JsonElementTypes.FLOAT.fromElement( element );
	}

	@Override
	public Float decode(Double value) {
		return NumberUtils.toFloat( value );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return other instanceof ElasticsearchFloatFieldCodec;
	}
}
