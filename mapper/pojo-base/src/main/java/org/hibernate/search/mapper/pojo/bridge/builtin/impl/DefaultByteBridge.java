/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultByteBridge extends AbstractPassThroughDefaultBridge<Byte> {

	public static final DefaultByteBridge INSTANCE = new DefaultByteBridge();

	private DefaultByteBridge() {
	}

	@Override
	protected String toString(Byte value) {
		return value.toString();
	}

	@Override
	protected Byte fromString(String value) {
		return ParseUtils.parseByte( value );
	}

}
