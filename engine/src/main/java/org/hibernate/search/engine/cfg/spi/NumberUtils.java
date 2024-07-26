/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class NumberUtils {

	private NumberUtils() {
	}

	public static BigDecimal toBigDecimal(Double value) {
		if ( value == null ) {
			return null;
		}
		return BigDecimal.valueOf( value );
	}

	public static BigInteger toBigInteger(Double value) {
		if ( value == null ) {
			return null;
		}
		return BigInteger.valueOf( value.longValue() );
	}

	public static Byte toByte(Double value) {
		if ( value == null ) {
			return null;
		}
		return value.byteValue();
	}

	public static Float toFloat(Double value) {
		if ( value == null ) {
			return null;
		}
		return value.floatValue();
	}

	public static Integer toInteger(Double value) {
		if ( value == null ) {
			return null;
		}
		return value.intValue();
	}

	public static Long toLong(Double value) {
		if ( value == null ) {
			return null;
		}
		return value.longValue();
	}

	public static Short toShort(Double value) {
		if ( value == null ) {
			return null;
		}
		return value.shortValue();
	}
}
