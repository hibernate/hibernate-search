package org.hibernate.search.util.json.internal.spi;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.util.common.annotation.Incubating;

import tools.jackson.databind.JsonNode;

/** Exact Jackson-native scalar construction and decimal conversion policy; no Java object mapping. */
@Incubating
public final class JsonNumbers {
	private JsonNumbers() {
	}

	/** Snapshot a number into an ordinary finite node, preserving integer/decimal precision and floating signed zero. */
	public static JsonNode fromNumber(Number value) {
		return org.hibernate.search.util.json.impl.JsonNumbers.fromNumber( value );
	}

	/** Validate the supported decimal scale and normalized exponent without expanding its value. */
	public static void validateDecimal(BigDecimal value) {
		org.hibernate.search.util.json.impl.JsonNumbers.validateDecimal( value );
	}

	/**
	 * Convert a mathematical integer exactly. Fractions fail; negative scale expansion is limited to 100000.
	 * Accepted storage can have a larger exponent than this conversion supports.
	 */
	public static BigInteger toBigInteger(BigDecimal value) {
		return org.hibernate.search.util.json.impl.JsonNumbers.toBigInteger( value );
	}

	/** Truncate fractional aggregation keys while bounding integer expansion. */
	public static BigInteger truncateToBigInteger(BigDecimal value) {
		return org.hibernate.search.util.json.impl.JsonNumbers.truncateToBigInteger( value );
	}
}
