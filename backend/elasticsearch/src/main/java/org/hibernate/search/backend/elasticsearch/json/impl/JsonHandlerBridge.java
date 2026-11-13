package org.hibernate.search.backend.elasticsearch.json.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchMiscLog;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Shared explicit user-library boundary. Ordinary DSL/transport processing does not use handlers. */
public final class JsonHandlerBridge {
	private static final JsonParsing PARSING = new JsonParsing();

	private JsonHandlerBridge() {
	}

	public static boolean isTrusted(JsonHandler<?> handler) {
		return handler == JacksonJsonHandlers.object() || handler == JacksonJsonHandlers.value();
	}

	public static <T> JsonNode importValue(JsonHandler<T> handler, T value, String operation) {
		return importValue( handler, value, operation, PARSING );
	}

	public static <T> JsonNode importValue(JsonHandler<T> handler, T value, String operation, JsonParsing parsing) {
		try {
			Class<T> javaType = Objects.requireNonNull( Objects.requireNonNull( handler ).javaType() );
			javaType.cast( Objects.requireNonNull( value ) );
			if ( isTrusted( handler ) ) {
				JsonNode node = (JsonNode) value;
				JsonTreeValidation.validateExternal( node );
				return node;
			}
			return parsing.parse( Objects.requireNonNull( handler.toJson( value ) ) );
		}
		catch (RuntimeException e) {
			throw QueryLog.INSTANCE.jsonHandlerOperationFailed( operation, e );
		}
	}

	public static <T> ObjectNode importObject(JsonHandler<T> handler, T value, String operation) {
		return requireObject( importValue( handler, value, operation ), operation );
	}

	public static <T> ObjectNode importObject(JsonHandler<T> handler, T value, String operation, JsonParsing parsing) {
		return requireObject( importValue( handler, value, operation, parsing ), operation );
	}

	/** Export only stable internal or externally validated trees; arbitrary inputs must use importValue first. */
	public static <T> T exportValue(JsonHandler<T> handler, JsonNode value, String operation) {
		return exportValue( handler, value, operation, JsonValidation.systemDefault() );
	}

	public static <T> T exportValue(JsonHandler<T> handler, JsonNode value, String operation, JsonValidation validation) {
		try {
			Class<T> javaType = Objects.requireNonNull( Objects.requireNonNull( handler ).javaType() );
			validation.validateInternal( Objects.requireNonNull( value ) );
			if ( isTrusted( handler ) ) {
				return javaType.cast( value );
			}
			return javaType.cast( Objects.requireNonNull( handler.fromJson( toJson( value, validation ) ) ) );
		}
		catch (RuntimeException e) {
			throw QueryLog.INSTANCE.jsonHandlerOperationFailed( operation, e );
		}
	}

	/** Absent optional object results return Java null without invoking the handler. */
	public static <T> T exportObject(JsonHandler<T> handler, JsonNode value, String operation) {
		return exportObject( handler, value, operation, JsonValidation.systemDefault() );
	}

	public static <T> T exportObject(JsonHandler<T> handler, JsonNode value, String operation, JsonValidation validation) {
		try {
			Objects.requireNonNull( Objects.requireNonNull( handler ).javaType() );
			return value == null ? null : exportValue( handler, requireObject( value, operation ), operation, validation );
		}
		catch (SearchException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw QueryLog.INSTANCE.jsonHandlerOperationFailed( operation, e );
		}
	}

	public static ObjectNode parseObject(String json, String operation) {
		return parseObject( json, operation, PARSING );
	}

	public static ObjectNode parseObject(String json, String operation, JsonParsing parsing) {
		try {
			return requireObject( parsing.parse( Objects.requireNonNull( json ) ), operation );
		}
		catch (SearchException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw QueryLog.INSTANCE.jsonHandlerOperationFailed( operation, e );
		}
	}

	public static JsonNode parseValue(String json) {
		return PARSING.parse( Objects.requireNonNull( json ) );
	}

	public static ObjectNode requireObject(JsonNode value, String operation) {
		if ( !( value instanceof ObjectNode object ) ) {
			throw QueryLog.INSTANCE.jsonHandlerOperationFailed( operation,
					ElasticsearchMiscLog.INSTANCE.expectedJsonObjectRoot() );
		}
		return object;
	}

	/** Internal/previously validated text boundary, preserving object members and nested nulls. */
	public static String toJson(JsonNode value) {
		return toJson( value, JsonValidation.systemDefault() );
	}

	public static String toJson(JsonNode value, JsonValidation validation) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			JsonBodies.ofInternal( value, validation ).writeTo( output );
		}
		catch (IOException e) {
			throw ElasticsearchMiscLog.INSTANCE.cannotWriteJson( e );
		}
		return output.toString( StandardCharsets.UTF_8 );
	}
}
