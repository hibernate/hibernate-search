package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

import tools.jackson.databind.JsonNode;

public abstract class AbstractElasticsearchFieldCodec<T> implements ElasticsearchFieldCodec<T> {

	protected final JsonCodecService json;

	public AbstractElasticsearchFieldCodec(JsonCodecService json) {
		this.json = json;
	}

	@Override
	public JsonNode fromJsonStringToElement(String value) {
		return json.decode( value, JsonNode.class );
	}

	@Override
	public String fromJsonElementToString(JsonNode value) {
		if ( value == null || value.isNull() ) {
			return null;
		}
		return json.write( value );
	}
}
