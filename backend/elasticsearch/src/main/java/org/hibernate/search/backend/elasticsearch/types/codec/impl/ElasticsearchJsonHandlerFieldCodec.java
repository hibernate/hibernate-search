package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.util.json.JsonHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.NullNode;

/** Binds one user JSON representation to native indexing, projection and runtime metadata. */
public class ElasticsearchJsonHandlerFieldCodec<T> implements ElasticsearchFieldCodec<T> {
	private final JsonCodecService json;
	private final JsonHandler<T> handler;
	private final Class<T> javaType;
	private final boolean arrayAsValue;

	public ElasticsearchJsonHandlerFieldCodec(JsonCodecService json, JsonHandler<T> handler) {
		this( json, handler, false );
	}

	public ElasticsearchJsonHandlerFieldCodec(JsonCodecService json, JsonHandler<T> handler, boolean arrayAsValue) {
		this.arrayAsValue = arrayAsValue;
		this.json = Objects.requireNonNull( json );
		this.handler = Objects.requireNonNull( handler );
		this.javaType = Objects.requireNonNull( handler.javaType() );
	}

	public Class<T> javaType() {
		return javaType;
	}

	@Override
	public JsonNode encode(T value) {
		return value == null
				? NullNode.instance
				: JsonHandlerBridge.importValue( handler, value, "native field indexing", json.parsing() );
	}

	@Override
	public T decode(JsonNode value) {
		return value == null || value.isNull() || value.isMissingNode()
				? null
				: JsonHandlerBridge.exportValue( handler, value, "native field projection", json.validation() );
	}

	@Override
	public T decode(Double value) {
		return value == null ? null : decode( DoubleNode.valueOf( value ) );
	}

	@Override
	public boolean canDecodeArrays() {
		return arrayAsValue;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		if ( other == this ) {
			return true;
		}
		if ( !( other instanceof ElasticsearchJsonHandlerFieldCodec<?> ) ) {
			return false;
		}
		ElasticsearchJsonHandlerFieldCodec<?> casted = (ElasticsearchJsonHandlerFieldCodec<?>) other;
		return arrayAsValue == casted.arrayAsValue
				&& handler == casted.handler && javaType == casted.javaType && json.equals( casted.json );
	}

	@Override
	public JsonNode fromJsonStringToElement(String value) {
		return json.decode( value, JsonNode.class );
	}

	@Override
	public String fromJsonElementToString(JsonNode value) {
		return json.write( value );
	}
}
