package org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.service;

import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.json.JsonHandler;

import com.google.gson.JsonObject;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Compile with only the backend requires: native Jackson and deprecated Gson APIs remain readable transitively. */
public final class JsonApiExample {
	private JsonApiExample() {
	}

	public static long requestValue() {
		ObjectNode body = JacksonJsonHandlers.object().fromJson( "{\"value\":42}" );
		return body.get( "value" ).longValue();
	}

	/** A custom handler with the same native type still executes its conversion callbacks. */
	public static ObjectNode customHandlerResult() {
		JsonHandler<ObjectNode> custom = new JsonHandler<>() {
			@Override
			public Class<ObjectNode> javaType() {
				return ObjectNode.class;
			}

			@Override
			public String toJson(ObjectNode value) {
				return JacksonJsonHandlers.object().toJson( value );
			}

			@Override
			public ObjectNode fromJson(String json) {
				ObjectNode result = JacksonJsonHandlers.object().fromJson( json );
				result.put( "custom", true );
				return result;
			}
		};
		try ( AuthorService service = new AuthorService() ) {
			int id = service.add( "custom source" );
			return service.source( custom, id );
		}
	}

	public static JsonNode nativeScalar() {
		return JacksonJsonHandlers.value().fromJson( "-0.0" );
	}

	@SuppressWarnings("deprecation")
	public static long deprecatedGsonValue() {
		JsonObject body = new JsonObject();
		body.addProperty( "value", 42 );
		return body.get( "value" ).getAsLong();
	}
}
