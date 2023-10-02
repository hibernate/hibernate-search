/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith} to display {@link Class} names in log
 * messages.
 *
 * @author Gunnar Morling
 */
public final class ClassFormatter {

	private final Class<?> clazz;

	public ClassFormatter(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public String toString() {
		return clazz != null ? clazz.getName() : "null";
	}
}
