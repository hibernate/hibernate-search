/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.math.BigInteger;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultBigIntegerBridge extends AbstractPassThroughDefaultBridge<BigInteger> {

	public static final DefaultBigIntegerBridge INSTANCE = new DefaultBigIntegerBridge();

	private DefaultBigIntegerBridge() {
	}

	@Override
	protected String toString(BigInteger value) {
		return value.toString();
	}

	@Override
	protected BigInteger fromString(String value) {
		return ParseUtils.parseBigInteger( value );
	}

}
