package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.engine.cfg.spi.NumberUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public final class ElasticsearchLongFieldCodec extends AbstractElasticsearchFieldCodec<Long> {

	public ElasticsearchLongFieldCodec(JsonCodecService json) {
		super( json );
	}

	@Override
	public JsonNode encode(Long value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		return JsonScalarConversions.toNode( value );
	}

	@Override
	public JsonNode encodeForAggregation(ElasticsearchSearchSyntax searchSyntax, Long value) {
		return searchSyntax.encodeLongForAggregation( value );
	}

	@Override
	public Long decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}
		return JsonElementTypes.LONG.fromElement( element );
	}

	@Override
	public Long decode(Double value) {
		return NumberUtils.toLong( value );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return other instanceof ElasticsearchLongFieldCodec;
	}
}
