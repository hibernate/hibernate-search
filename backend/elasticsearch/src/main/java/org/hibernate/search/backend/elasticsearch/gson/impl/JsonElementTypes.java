/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

	public static final JsonElementType<Byte> BYTE = new JsonNumberType<Byte>() {
		@Override
		protected Byte nullUnsafeFromNumber(JsonPrimitive primitive) {
			return primitive.getAsByte();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(Byte)";
		}
	};

	public static final JsonElementType<Short> SHORT = new JsonNumberType<Short>() {
		@Override
		protected Short nullUnsafeFromNumber(JsonPrimitive primitive) {
			return primitive.getAsShort();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(Short)";
		}
	};

	public static final JsonElementType<BigDecimal> BIG_DECIMAL = new JsonNumberType<BigDecimal>() {
		@Override
		protected BigDecimal nullUnsafeFromNumber(JsonPrimitive primitive) {
			return primitive.getAsBigDecimal();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(BigDecimal)";
		}
	};

	public static final JsonElementType<BigInteger> BIG_INTEGER = new JsonNumberType<BigInteger>() {
		@Override
		protected BigInteger nullUnsafeFromNumber(JsonPrimitive primitive) {
			return primitive.getAsBigInteger();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName() + "(BigInteger)";
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonElement element) {
			return element.isJsonPrimitive();
		}
	};

}
