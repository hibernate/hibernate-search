/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import java.math.BigInteger;

public class NumberScaleConstants {

	public static final BigInteger MIN_LONG_AS_BIGINTEGER = BigInteger.valueOf( Long.MIN_VALUE );
	public static final BigInteger MAX_LONG_AS_BIGINTEGER = BigInteger.valueOf( Long.MAX_VALUE );

	private NumberScaleConstants() {
		// Private constructor, do not use
	}
}
