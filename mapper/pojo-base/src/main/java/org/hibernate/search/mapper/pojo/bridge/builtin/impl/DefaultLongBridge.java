/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultLongBridge extends AbstractPassThroughDefaultBridge<Long> {

	public static final DefaultLongBridge INSTANCE = new DefaultLongBridge();

	private DefaultLongBridge() {
	}

	@Override
	protected String toString(Long value) {
		return value.toString();
	}

	@Override
	protected Long fromString(String value) {
		return ParseUtils.parseLong( value );
	}

}
