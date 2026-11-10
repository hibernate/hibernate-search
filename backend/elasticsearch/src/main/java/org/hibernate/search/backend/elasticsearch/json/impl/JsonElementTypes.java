package org.hibernate.search.backend.elasticsearch.json.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Strict backend type selection; scalar conversion policies are separate from singleton-array adaptation. */
public final class JsonElementTypes {
	private JsonElementTypes() {
	}

	// Separate type classes keep navigation and scalar readers distinct at the shared accessor call sites.
	public static final JsonElementType<ObjectNode> OBJECT = new JsonElementType<>() {
		@Override
		protected ObjectNode nullUnsafeFromElement(JsonNode value) {
			return (ObjectNode) value;
		}

		@Override
		protected JsonNode nullUnsafeToElement(ObjectNode value) {
			return value;
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonNode value) {
			return value.isObject();
		}

		@Override
		public String toString() {
			return "ObjectNode";
		}
	};

	public static final JsonElementType<ArrayNode> ARRAY = new JsonElementType<>() {
		@Override
		protected ArrayNode nullUnsafeFromElement(JsonNode value) {
			return (ArrayNode) value;
		}

		@Override
		protected JsonNode nullUnsafeToElement(ArrayNode value) {
			return value;
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonNode value) {
			return value.isArray();
		}

		@Override
		public String toString() {
			return "ArrayNode";
		}
	};

	public static final JsonElementType<String> STRING = new JsonElementType<>() {
		@Override
		protected String nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asString( value );
		}

		@Override
		protected JsonNode nullUnsafeToElement(String value) {
			return JsonScalarConversions.toNode( value );
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonNode value) {
			return value.isString();
		}

		@Override
		public String toString() {
			return "JsonNode(String)";
		}
	};

	public static final JsonElementType<Boolean> BOOLEAN = new JsonElementType<>() {
		@Override
		protected Boolean nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asBoolean( value );
		}

		@Override
		protected JsonNode nullUnsafeToElement(Boolean value) {
			return JsonScalarConversions.toNode( value );
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonNode value) {
			return value.isBoolean();
		}

		@Override
		public String toString() {
			return "JsonNode(Boolean)";
		}
	};

	private abstract static class JsonNumberType<T extends Number> extends JsonElementType<T> {
		@Override
		protected JsonNode nullUnsafeToElement(T value) {
			return JsonScalarConversions.toNode( value );
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonNode value) {
			return value.isNumber();
		}
	}

	public static final JsonElementType<Integer> INTEGER = new JsonNumberType<>() {
		@Override
		protected Integer nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asInteger( value );
		}

		@Override
		public String toString() {
			return "JsonNode(Integer)";
		}
	};

	public static final JsonElementType<Long> LONG = new JsonNumberType<>() {
		@Override
		protected Long nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asLong( value );
		}

		@Override
		public String toString() {
			return "JsonNode(Long)";
		}
	};

	public static final JsonElementType<Float> FLOAT = new JsonNumberType<>() {
		@Override
		protected Float nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asFloat( value );
		}

		@Override
		public String toString() {
			return "JsonNode(Float)";
		}
	};

	public static final JsonElementType<Double> DOUBLE = new JsonNumberType<>() {
		@Override
		protected Double nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asDouble( value );
		}

		@Override
		public String toString() {
			return "JsonNode(Double)";
		}
	};

	public static final JsonElementType<Byte> BYTE = new JsonNumberType<>() {
		@Override
		protected Byte nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asByte( value );
		}

		@Override
		public String toString() {
			return "JsonNode(Byte)";
		}
	};

	public static final JsonElementType<Short> SHORT = new JsonNumberType<>() {
		@Override
		protected Short nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asShort( value );
		}

		@Override
		public String toString() {
			return "JsonNode(Short)";
		}
	};

	public static final JsonElementType<BigDecimal> BIG_DECIMAL = new JsonNumberType<>() {
		@Override
		protected BigDecimal nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asBigDecimal( value );
		}

		@Override
		public String toString() {
			return "JsonNode(BigDecimal)";
		}
	};

	public static final JsonElementType<BigInteger> BIG_INTEGER = new JsonNumberType<>() {
		@Override
		protected BigInteger nullUnsafeFromElement(JsonNode value) {
			return JsonScalarConversions.asBigInteger( value );
		}

		@Override
		public String toString() {
			return "JsonNode(BigInteger)";
		}

		@Override
		protected boolean nullUnsafeIsInstance(JsonNode value) {
			return value.isNumber() || value.isString() || value.isBoolean();
		}
	};

}
