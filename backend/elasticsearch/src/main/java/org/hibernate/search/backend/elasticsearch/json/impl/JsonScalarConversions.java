package org.hibernate.search.backend.elasticsearch.json.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchMiscLog;
import org.hibernate.search.util.json.internal.spi.JsonNumbers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

/**
 * Internal scalar construction/coercion; it never maps DTOs or invokes application JSON handlers.
 */
public final class JsonScalarConversions {

	private JsonScalarConversions() {
	}

	public static JsonNode toNode(Object value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		if ( value instanceof JsonNode node ) {
			return node;
		}
		if ( value instanceof String text ) {
			return StringNode.valueOf( text );
		}
		if ( value instanceof Character character ) {
			return StringNode.valueOf( character.toString() );
		}
		if ( value instanceof Boolean bool ) {
			return BooleanNode.valueOf( bool );
		}
		if ( value instanceof Number number ) {
			return JsonNumbers.fromNumber( number );
		}
		throw ElasticsearchMiscLog.INSTANCE.unsupportedJsonScalarType( value.getClass().getName() );
	}

	private static JsonNode scalar(JsonNode value) {
		if ( value == null || value.isNull() || value.isMissingNode() ) {
			throw ElasticsearchMiscLog.INSTANCE.expectedJsonScalar();
		}
		while ( value.isArray() ) {
			if ( value.size() != 1 ) {
				throw ElasticsearchMiscLog.INSTANCE.expectedSingletonJsonScalarArray();
			}
			value = value.get( 0 );
		}
		if ( !value.isString() && !value.isBoolean() && !value.isNumber() ) {
			throw ElasticsearchMiscLog.INSTANCE.expectedJsonScalar();
		}
		return value;
	}

	public static String asString(JsonNode value) {
		JsonNode node = scalar( value );
		if ( node.isString() ) {
			return node.asString();
		}
		if ( node.isBoolean() ) {
			return Boolean.toString( node.booleanValue() );
		}
		return node.numberValue().toString();
	}

	public static boolean asBoolean(JsonNode value) {
		JsonNode node = scalar( value );
		return node.isBoolean() ? node.booleanValue() : Boolean.parseBoolean( asString( node ) );
	}

	public static Number asNumber(JsonNode value) {
		JsonNode node = scalar( value );
		if ( node.isNumber() ) {
			return node.numberValue();
		}
		if ( node.isString() ) {
			return new BigDecimal( node.asString() );
		}
		throw ElasticsearchMiscLog.INSTANCE.jsonBooleanIsNotNumber();
	}

	// Multiplication by10^64 has zero low64bits. Avoid enormous decimal expansion when narrowing.
	private static Number narrowingNumber(JsonNode node) {
		Number value = node.numberValue();
		return value instanceof BigDecimal decimal && decimal.scale() <= -64 ? BigInteger.ZERO : value;
	}

	public static int asInteger(JsonNode value) {
		JsonNode node = scalar( value );
		return node.isNumber() ? narrowingNumber( node ).intValue() : Integer.parseInt( asString( node ) );
	}

	public static long asLong(JsonNode value) {
		JsonNode node = scalar( value );
		return node.isNumber() ? narrowingNumber( node ).longValue() : Long.parseLong( asString( node ) );
	}

	public static short asShort(JsonNode value) {
		JsonNode node = scalar( value );
		return node.isNumber() ? narrowingNumber( node ).shortValue() : Short.parseShort( asString( node ) );
	}

	public static byte asByte(JsonNode value) {
		JsonNode node = scalar( value );
		return node.isNumber() ? narrowingNumber( node ).byteValue() : Byte.parseByte( asString( node ) );
	}

	public static float asFloat(JsonNode value) {
		JsonNode node = scalar( value );
		return node.isNumber() ? node.numberValue().floatValue() : Float.parseFloat( asString( node ) );
	}

	public static double asDouble(JsonNode value) {
		JsonNode node = scalar( value );
		return node.isNumber() ? node.numberValue().doubleValue() : Double.parseDouble( asString( node ) );
	}

	public static BigDecimal asBigDecimal(JsonNode value) {
		JsonNode node = scalar( value );
		BigDecimal result;
		if ( node.isNumber() ) {
			Number number = node.numberValue();
			result = number instanceof BigDecimal decimal ? decimal
					: number instanceof BigInteger integer ? new BigDecimal( integer )
					: new BigDecimal( number.toString() );
		}
		else {
			result = new BigDecimal( asString( node ) );
		}
		JsonNumbers.validateDecimal( result );
		return result;
	}

	/**
	 * Value-based exact conversion accepts integral exponent/decimal values; textual integers retain strict parsing.
	 */
	public static BigInteger asBigInteger(JsonNode value) {
		JsonNode node = scalar( value );
		if ( node.isString() ) {
			return new BigInteger( node.asString() );
		}
		if ( !node.isNumber() ) {
			return new BigInteger( asString( node ) );
		}
		Number number = node.numberValue();
		if ( number instanceof BigInteger integer ) {
			return integer;
		}
		if ( number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long ) {
			return BigInteger.valueOf( number.longValue() );
		}
		return JsonNumbers.toBigInteger( asBigDecimal( node ) );
	}

	/**
	 * Tree equality preserves mathematical numeric equality across standard numeric node classes.
	 */
	public static boolean equivalent(JsonNode left, JsonNode right) {
		if ( left == null || right == null ) {
			return left == right;
		}
		return left.equals( (a,
				b) -> a.isNumber() && b.isNumber() ? asBigDecimal( a ).compareTo( asBigDecimal( b ) ) : a.equals( b ) ? 0 : 1,
				right );
	}
}
