/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.util.Arrays;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultByteArrayBridge extends AbstractPassThroughDefaultBridge<byte[]> {

	public static final DefaultByteArrayBridge INSTANCE = new DefaultByteArrayBridge();

	private DefaultByteArrayBridge() {
	}

	@Override
	protected String toString(byte[] value) {
		return Arrays.toString( value );
	}

	@Override
	protected byte[] fromString(String value) {
		return ParseUtils.parseBytePrimitiveArray( value );
	}
}
