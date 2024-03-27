/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;


/**
 * Log category to be used with {@link LoggerFactory#make(Class, LogCategory)}.
 *
 * @author Gunnar Morling
 */
public final class LogCategory {

	private final String name;

	public LogCategory(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

}
