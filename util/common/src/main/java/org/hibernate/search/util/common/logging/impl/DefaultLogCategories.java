/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

/**
 * Log categories to be used with {@link LoggerFactory#make(Class, LogCategory)}.
 *
 * @author Gunnar Morling
 */
public final class DefaultLogCategories {

	private DefaultLogCategories() {
	}

	/**
	 * Category for logging executed search queries at the TRACE level.
	 */
	public static final LogCategory QUERY = new LogCategory( "org.hibernate.search.query" );

}
