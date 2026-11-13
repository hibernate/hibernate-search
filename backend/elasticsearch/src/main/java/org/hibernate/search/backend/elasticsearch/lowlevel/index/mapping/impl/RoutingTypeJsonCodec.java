package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.json.internal.spi.JsonCodec;
import org.hibernate.search.util.json.internal.spi.JsonCodecs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

public final class RoutingTypeJsonCodec implements JsonCodec<RoutingType> {
	public RoutingType decode(JsonNode value) {
		if ( JsonCodecs.isNull( value ) ) {
			return null;
		}
		RoutingType result = null;
		for ( var e : ( (ObjectNode) value ).properties() ) {
			if ( !e.getKey().equals( "required" ) ) {
				throw new AssertionFailure(
						"Unexpected property for attribute of type " + RoutingType.class + ": " + e.getKey() );
			}
			if ( !e.getValue().isBoolean() ) {
				throw new IllegalArgumentException( "Expected routing required boolean" );
			}
			result = e.getValue().asBoolean() ? RoutingType.REQUIRED : RoutingType.OPTIONAL;
		}
		return result;
	}

	public JsonNode encode(RoutingType value, boolean serializeNulls) {
		if ( value == null ) {
			return NullNode.instance;
		}
		if ( value != RoutingType.REQUIRED ) {
			throw new AssertionFailure( "Unexpected value for attribute of type " + RoutingType.class + ": " + value );
		}
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.put( "required", true );
		return result;
	}
}
