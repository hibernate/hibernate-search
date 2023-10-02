/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultIntegerBridge extends AbstractPassThroughDefaultBridge<Integer> {

	public static final DefaultIntegerBridge INSTANCE = new DefaultIntegerBridge();

	private DefaultIntegerBridge() {
	}

	@Override
	protected String toString(Integer value) {
		return value.toString();
	}

	@Override
	protected Integer fromString(String value) {
		return ParseUtils.parseInteger( value );
	}

}
