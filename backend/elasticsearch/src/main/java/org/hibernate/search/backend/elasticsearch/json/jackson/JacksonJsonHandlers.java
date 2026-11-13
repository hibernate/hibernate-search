package org.hibernate.search.backend.elasticsearch.json.jackson;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Stable Jackson 3 adapters. Search recognizes these exact instances for direct transfer. */
public final class JacksonJsonHandlers {
	private JacksonJsonHandlers() {
	}

	public static JsonHandler<ObjectNode> object() {
		return ObjectHandler.INSTANCE;
	}

	public static JsonHandler<JsonNode> value() {
		return ValueHandler.INSTANCE;
	}

	private enum ObjectHandler implements JsonHandler<ObjectNode> {
		INSTANCE;

		@Override
		public Class<ObjectNode> javaType() {
			return ObjectNode.class;
		}

		@Override
		public String toJson(ObjectNode value) {
			JsonTreeValidation.validateExternal( Objects.requireNonNull( value ) );
			return JsonHandlerBridge.toJson( value );
		}

		@Override
		public ObjectNode fromJson(String json) {
			return JsonHandlerBridge.parseObject( json, "Jackson JSON object import" );
		}
	}

	private enum ValueHandler implements JsonHandler<JsonNode> {
		INSTANCE;

		@Override
		public Class<JsonNode> javaType() {
			return JsonNode.class;
		}

		@Override
		public String toJson(JsonNode value) {
			JsonTreeValidation.validateExternal( Objects.requireNonNull( value ) );
			return JsonHandlerBridge.toJson( value );
		}

		@Override
		public JsonNode fromJson(String json) {
			return JsonHandlerBridge.parseValue( json );
		}
	}
}
