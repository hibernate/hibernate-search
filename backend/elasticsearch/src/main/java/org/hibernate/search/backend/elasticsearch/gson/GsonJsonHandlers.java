package org.hibernate.search.backend.elasticsearch.gson;

import java.util.Objects;

import org.hibernate.search.util.json.JsonHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

/** Stable strict Gson adapters; conversion occurs only at an explicit user boundary. */
public final class GsonJsonHandlers {
	private static final Gson GSON = new GsonBuilder().setStrictness( Strictness.STRICT ).create();

	private GsonJsonHandlers() {
	}

	public static JsonHandler<JsonObject> object() {
		return ObjectHandler.INSTANCE;
	}

	public static JsonHandler<JsonElement> value() {
		return ValueHandler.INSTANCE;
	}

	private enum ObjectHandler implements JsonHandler<JsonObject> {
		INSTANCE;

		@Override
		public Class<JsonObject> javaType() {
			return JsonObject.class;
		}

		@Override
		public String toJson(JsonObject value) {
			return Objects.requireNonNull( value ).toString();
		}

		@Override
		public JsonObject fromJson(String json) {
			return Objects.requireNonNull( GSON.fromJson( Objects.requireNonNull( json ), JsonObject.class ) );
		}
	}

	private enum ValueHandler implements JsonHandler<JsonElement> {
		INSTANCE;

		@Override
		public Class<JsonElement> javaType() {
			return JsonElement.class;
		}

		@Override
		public String toJson(JsonElement value) {
			return Objects.requireNonNull( value ).toString();
		}

		@Override
		public JsonElement fromJson(String json) {
			return Objects.requireNonNull( GSON.fromJson( Objects.requireNonNull( json ), JsonElement.class ) );
		}
	}
}
