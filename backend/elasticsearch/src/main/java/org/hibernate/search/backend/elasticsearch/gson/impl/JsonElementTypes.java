/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class JsonElementTypes {

	private JsonElementTypes() {
		// Private constructor, do not use.
	}

	public static final JsonElementType<JsonObject> OBJECT = new JsonElementType<JsonObject>() {
		@Override
		protected JsonObject nullUnsafeFromElement(JsonElement element) {
			return element.isJsonNull() ? null : element.getAsJsonObject();
		}

		@Override
		protected JsonElement nullUnsafeToElement(JsonObject value) {
			return value;
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonElement element) {
			return element.isJsonObject();
		}

		@Override
		public String toString() {
			return JsonObject.class.getSimpleName();
		}
	};

	public static final JsonElementType<JsonArray> ARRAY = new JsonElementType<JsonArray>() {
		@Override
		protected JsonArray nullUnsafeFromElement(JsonElement element) {
			return element.isJsonNull() ? null : element.getAsJsonArray();
		}

		@Override
		protected JsonElement nullUnsafeToElement(JsonArray value) {
			return value;
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonElement element) {
			return element.isJsonArray();
		}

		@Override
		public String toString() {
			return JsonArray.class.getSimpleName();
		}
	};

	public static final JsonElementType<JsonPrimitive> PRIMITIVE = new JsonElementType<JsonPrimitive>() {
		@Override
		protected JsonPrimitive nullUnsafeFromElement(JsonElement element) {
			return element.isJsonNull() ? null : element.getAsJsonPrimitive();
		}

		@Override
		protected JsonElement nullUnsafeToElement(JsonPrimitive value) {
			return value;
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonElement element) {
			return element.isJsonPrimitive();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName();
		}
	};

	public static final JsonElementType<String> STRING = new JsonElementType<String>() {
		@Override
		protected String nullUnsafeFromElement(JsonElement element) {
			return element.isJsonNull() ? null : element.getAsJsonPrimitive().getAsString();
		}

		@Override
		protected JsonElement nullUnsafeToElement(String value) {
			return new JsonPrimitive( value );
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonElement element) {
			return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(String)";
		}
	};

	public static final JsonElementType<Boolean> BOOLEAN = new JsonElementType<Boolean>() {
		@Override
		protected Boolean nullUnsafeFromElement(JsonElement element) {
			// Use asInt instead of asInteger to fail fast
			return element.isJsonNull() ? null : element.getAsJsonPrimitive().getAsBoolean();
		}

		@Override
		protected JsonElement nullUnsafeToElement(Boolean value) {
			return new JsonPrimitive( value );
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonElement element) {
			return element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(Boolean)";
		}
	};

	private abstract static class JsonNumberType<T extends Number> extends JsonElementType<T> {
		@Override
		protected T nullUnsafeFromElement(JsonElement element) {
			return element.isJsonNull() ? null : nullUnsafeFromNumber( element.getAsJsonPrimitive() );
		}

		protected abstract T nullUnsafeFromNumber(JsonPrimitive primitive);

		@Override
		protected JsonElement nullUnsafeToElement(T value) {
			return new JsonPrimitive( value );
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonElement element) {
			return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
		}

	}

	public static final JsonElementType<Integer> INTEGER = new JsonNumberType<Integer>() {
		@Override
		protected Integer nullUnsafeFromNumber(JsonPrimitive primitive) {
			return primitive.getAsInt();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(Integer)";
		}
	};

	public static final JsonElementType<Long> LONG = new JsonNumberType<Long>() {
		@Override
		protected Long nullUnsafeFromNumber(JsonPrimitive primitive) {
			return primitive.getAsLong();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(Long)";
		}
	};

	public static final JsonElementType<Float> FLOAT = new JsonNumberType<Float>() {
		@Override
		protected Float nullUnsafeFromNumber(JsonPrimitive primitive) {
			return primitive.getAsFloat();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(Float)";
		}
	};

	public static final JsonElementType<Double> DOUBLE = new JsonNumberType<Double>() {
		@Override
		protected Double nullUnsafeFromNumber(JsonPrimitive primitive) {
			return primitive.getAsDouble();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(Double)";
		}
	};

	public static final JsonElementType<JsonNull> NULL = new JsonElementType<JsonNull>() {
		@Override
		protected JsonNull nullUnsafeFromElement(JsonElement element) {
			return element.getAsJsonNull();
		}

		@Override
		protected JsonElement nullUnsafeToElement(JsonNull element) {
			return element;
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonElement element) {
			return element.isJsonNull();
		}

		@Override
		public String toString() {
			return JsonNull.class.getSimpleName();
		}
	};
}
