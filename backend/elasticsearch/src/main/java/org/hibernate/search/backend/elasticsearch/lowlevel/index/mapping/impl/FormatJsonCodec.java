package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.util.json.internal.spi.JsonCodec;
import org.hibernate.search.util.json.internal.spi.JsonCodecs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

public final class FormatJsonCodec implements JsonCodec<List<String>> {
	public List<String> decode(JsonNode value) {
		if ( JsonCodecs.isNull( value ) ) {
			return null;
		}
		if ( !( value.isString() || value.isNumber() ) ) {
			throw new IllegalArgumentException( "Expected format string" );
		}
		return Arrays.asList( JsonCodecs.STRING.decode( value ).split( "\\|\\|" ) );
	}

	public JsonNode encode(List<String> value, boolean serializeNulls) {
		return value == null ? NullNode.instance : StringNode.valueOf( String.join( "||", value ) );
	}
}
