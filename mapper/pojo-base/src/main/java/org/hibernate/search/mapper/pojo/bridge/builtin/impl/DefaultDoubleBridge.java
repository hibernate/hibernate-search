/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultDoubleBridge extends AbstractPassThroughDefaultBridge<Double> {

	public static final DefaultDoubleBridge INSTANCE = new DefaultDoubleBridge();

	private DefaultDoubleBridge() {
	}

	@Override
	protected String toString(Double value) {
		return value.toString();
	}

	@Override
	protected Double fromString(String value) {
		return ParseUtils.parseDouble( value );
	}

}
