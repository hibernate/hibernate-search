/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultBooleanBridge extends AbstractPassThroughDefaultBridge<Boolean> {

	public static final DefaultBooleanBridge INSTANCE = new DefaultBooleanBridge();

	private DefaultBooleanBridge() {
	}

	@Override
	protected String toString(Boolean value) {
		return value.toString();
	}

	@Override
	protected Boolean fromString(String value) {
		return ParseUtils.parseBoolean( value );
	}

}
