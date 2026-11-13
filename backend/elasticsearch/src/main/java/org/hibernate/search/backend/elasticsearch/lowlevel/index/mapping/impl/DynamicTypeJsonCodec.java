package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.json.internal.spi.JsonCodec;
import org.hibernate.search.util.json.internal.spi.JsonCodecs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

public final class DynamicTypeJsonCodec implements JsonCodec<DynamicType> {
	public DynamicType decode(JsonNode value) {
		if ( JsonCodecs.isNull( value ) ) {
			return null;
		}
		if ( !value.isString() ) {
			throw new IllegalArgumentException( "Expected dynamic string" );
		}
		return switch ( value.asString() ) {
			case "true" -> DynamicType.TRUE;
			case "false" -> DynamicType.FALSE;
			case "strict" -> DynamicType.STRICT;
			default -> throw new AssertionFailure( "Unknown dynamic type value: " + value );
		};
	}

	public JsonNode encode(DynamicType value, boolean serializeNulls) {
		return value == null ? NullNode.instance : StringNode.valueOf( switch ( value ) {
			case TRUE -> "true";
			case FALSE -> "false";
			case STRICT -> "strict";
		} );
	}
}
