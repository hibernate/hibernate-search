/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import org.hibernate.search.util.common.logging.impl.LogCategory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Log categories to be used with {@link LoggerFactory#make(Class, LogCategory)}.
 *
 */
public final class ElasticsearchLogCategories {

	private ElasticsearchLogCategories() {
	}

	/**
	 * This is the category of the Logger used to print out executed Elasticsearch requests,
	 * along with the execution time.
	 * <p>
	 * To enable the logger, the category needs to be enabled at TRACE level.
	 */
	public static final LogCategory REQUEST = new LogCategory( "org.hibernate.search.elasticsearch.request" );

}
