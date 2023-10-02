/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultShortBridge extends AbstractPassThroughDefaultBridge<Short> {

	public static final DefaultShortBridge INSTANCE = new DefaultShortBridge();

	private DefaultShortBridge() {
	}

	@Override
	protected String toString(Short value) {
		return value.toString();
	}

	@Override
	protected Short fromString(String value) {
		return ParseUtils.parseShort( value );
	}

}
