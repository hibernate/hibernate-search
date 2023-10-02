/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

public final class DefaultStringBridge extends AbstractPassThroughDefaultBridge<String> {

	public static final DefaultStringBridge INSTANCE = new DefaultStringBridge();

	private DefaultStringBridge() {
	}

	@Override
	protected String toString(String value) {
		return value;
	}

	@Override
	protected String fromString(String value) {
		return value;
	}

}
