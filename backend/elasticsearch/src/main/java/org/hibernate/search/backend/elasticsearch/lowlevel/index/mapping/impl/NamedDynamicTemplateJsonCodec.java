package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import org.hibernate.search.util.json.internal.spi.JsonCodec;
import org.hibernate.search.util.json.internal.spi.JsonCodecRegistry;
import org.hibernate.search.util.json.internal.spi.JsonCodecs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

public final class NamedDynamicTemplateJsonCodec implements JsonCodec<NamedDynamicTemplate> {
	private final JsonCodec<DynamicTemplate> template;

	public NamedDynamicTemplateJsonCodec(JsonCodecRegistry registry) {
		template = registry.reference( DynamicTemplate.class );
	}

	public NamedDynamicTemplate decode(JsonNode value) {
		if ( JsonCodecs.isNull( value ) ) {
			return null;
		}
		ObjectNode object = (ObjectNode) value;
		if ( object.size() != 1 ) {
			throw new IllegalArgumentException( "Expected exactly one named dynamic template" );
		}
		var e = object.properties().iterator().next();
		return new NamedDynamicTemplate( e.getKey(), template.decode( e.getValue() ) );
	}

	public JsonNode encode(NamedDynamicTemplate value, boolean serializeNulls) {
		if ( value == null ) {
			return NullNode.instance;
		}
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		JsonCodecs.put( result, value.name, template.encode( value.template, serializeNulls ), serializeNulls );
		return result;
	}
}
