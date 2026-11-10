package org.hibernate.search.util.json.internal.spi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.logging.impl.JsonLog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

/** Composable codecs for explicitly selected tree, scalar, list and string-keyed map types. */
@Incubating
public final class JsonCodecs {
	private JsonCodecs() {
	}

	public static final JsonCodec<JsonNode> ELEMENT = new JsonCodec<>() {
		public JsonNode decode(JsonNode value) {
			return value;
		}

		public JsonNode encode(JsonNode value, boolean serializeNulls) {
			return copy( value, serializeNulls );
		}
	};
	public static final JsonCodec<String> STRING = scalar( JsonCodecs::readString, StringNode::valueOf );
	public static final JsonCodec<Boolean> BOOLEAN = scalar( JsonCodecs::readBoolean, BooleanNode::valueOf, false );
	public static final JsonCodec<Integer> INTEGER = scalar( v -> v.isNumber()
			? narrowingNumber( v ).intValue()
			: Integer.parseInt( readString( v ) ), JsonNumbers::fromNumber );
	public static final JsonCodec<Long> LONG = scalar( v -> v.isNumber()
			? narrowingNumber( v ).longValue()
			: Long.parseLong( readString( v ) ), JsonNumbers::fromNumber );
	public static final JsonCodec<Double> DOUBLE = scalar( v -> v.isNumber()
			? v.numberValue().doubleValue()
			: Double.parseDouble( readString( v ) ), JsonNumbers::fromNumber );
	public static final JsonCodec<Float> FLOAT = scalar( v -> v.isNumber()
			? v.numberValue().floatValue()
			: Float.parseFloat( readString( v ) ), JsonNumbers::fromNumber );

	private static Number narrowingNumber(JsonNode value) {
		Number number = value.numberValue();
		// Powers of ten with exponent >=64 have zero low64bits; avoid enormous decimal expansion.
		return number instanceof BigDecimal decimal && decimal.scale() <= -64 ? 0 : number;
	}

	private static String readString(JsonNode value) {
		if ( value.isString() ) {
			return value.asString();
		}
		if ( value.isNumber() ) {
			return value.numberValue().toString();
		}
		if ( value.isBoolean() ) {
			return Boolean.toString( value.asBoolean() );
		}
		throw JsonLog.INSTANCE.expectedScalar();
	}

	private static Boolean readBoolean(JsonNode value) {
		if ( !value.isBoolean() && !value.isString() ) {
			throw JsonLog.INSTANCE.expectedBoolean();
		}
		return value.isBoolean() ? value.asBoolean() : Boolean.parseBoolean( value.asString() );
	}

	public static boolean isNull(JsonNode value) {
		return value == null || value.isNull();
	}

	private static JsonNode scalarValue(JsonNode value) {
		while ( value.isArray() ) {
			if ( value.size() != 1 ) {
				throw JsonLog.INSTANCE.expectedSingletonArray();
			}
			value = value.get( 0 );
		}
		return value;
	}

	private static <T> JsonCodec<T> scalar(Function<JsonNode, T> read, Function<T, JsonNode> write) {
		return scalar( read, write, true );
	}

	private static <T> JsonCodec<T> scalar(Function<JsonNode, T> read, Function<T, JsonNode> write,
			boolean unwrapSingletonArrays) {
		return new JsonCodec<>() {
			public T decode(JsonNode value) {
				return isNull( value ) ? null : read.apply( unwrapSingletonArrays ? scalarValue( value ) : value );
			}

			public JsonNode encode(T value, boolean serializeNulls) {
				return value == null ? NullNode.instance : write.apply( value );
			}
		};
	}

	public static void put(ObjectNode target, String name, JsonNode value, boolean serializeNulls) {
		if ( serializeNulls || !isNull( value ) ) {
			target.set( name, value );
		}
	}

	/** Structural copy applies member-null omission recursively; null array slots are retained. */
	public static JsonNode copy(JsonNode value, boolean serializeNulls) {
		if ( isNull( value ) ) {
			return NullNode.instance;
		}
		if ( value instanceof ObjectNode object ) {
			ObjectNode result = JsonNodes.objectNode();
			for ( var entry : object.properties() ) {
				put( result, entry.getKey(), copy( entry.getValue(), serializeNulls ), serializeNulls );
			}
			return result;
		}
		if ( value instanceof ArrayNode array ) {
			ArrayNode result = JsonNodes.arrayNode();
			for ( JsonNode entry : array ) {
				result.add( copy( entry, serializeNulls ) );
			}
			return result;
		}
		return value;
	}

	public static <T> JsonCodec<List<T>> list(JsonCodec<T> element) {
		return new JsonCodec<>() {
			public List<T> decode(JsonNode value) {
				if ( isNull( value ) ) {
					return null;
				}
				if ( !( value instanceof ArrayNode array ) ) {
					throw JsonLog.INSTANCE.expectedArray();
				}
				List<T> result = new ArrayList<>();
				for ( JsonNode entry : array ) {
					result.add( element.decode( entry ) );
				}
				return result;
			}

			public JsonNode encode(List<T> value, boolean serializeNulls) {
				if ( value == null ) {
					return NullNode.instance;
				}
				ArrayNode result = JsonNodes.arrayNode();
				for ( T entry : value ) {
					result.add( element.encode( entry, serializeNulls ) );
				}
				return result;
			}
		};
	}

	public static <T> JsonCodec<Map<String, T>> map(JsonCodec<T> element) {
		return new JsonCodec<>() {
			public Map<String, T> decode(JsonNode value) {
				if ( isNull( value ) ) {
					return null;
				}
				if ( !( value instanceof ObjectNode object ) ) {
					throw JsonLog.INSTANCE.expectedObject();
				}
				Map<String, T> result = new LinkedHashMap<>();
				for ( var entry : object.properties() ) {
					result.put( entry.getKey(), element.decode( entry.getValue() ) );
				}
				return result;
			}

			public JsonNode encode(Map<String, T> value, boolean serializeNulls) {
				if ( value == null ) {
					return NullNode.instance;
				}
				ObjectNode result = JsonNodes.objectNode();
				for ( var entry : value.entrySet() ) {
					put( result, entry.getKey(), element.encode( entry.getValue(), serializeNulls ), serializeNulls );
				}
				return result;
			}
		};
	}
}
