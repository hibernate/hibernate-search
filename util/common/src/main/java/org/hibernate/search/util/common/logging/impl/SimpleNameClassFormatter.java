/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

public final class SimpleNameClassFormatter {

	private final Class<?> clazz;

	public SimpleNameClassFormatter(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public String toString() {
		return clazz != null ? clazz.getSimpleName() : "null";
	}
}
