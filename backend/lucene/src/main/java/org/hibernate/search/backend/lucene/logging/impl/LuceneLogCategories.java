/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import org.hibernate.search.util.common.logging.impl.LogCategory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Log categories to be used with {@link LoggerFactory#make(Class, LogCategory)}.
 *
 * @author Gunnar Morling
 */
public final class LuceneLogCategories {

	private LuceneLogCategories() {
	}

	public static final String INFOSTREAM_LOGGER_CATEGORY_NAME = "org.hibernate.search.backend.lucene.infostream";

	/**
	 * This is the category of the Logger used to print out the Lucene infostream.
	 * <p>
	 * To enable the logger, the category needs to be enabled at TRACE level and configuration
	 * property {@code org.hibernate.search.backend.configuration.impl.IndexWriterSetting#INFOSTREAM}
	 * needs to be enabled on the index.
	 *
	 * @see org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings#IO_WRITER_INFOSTREAM
	 */
	public static final LogCategory INFOSTREAM_LOGGER_CATEGORY = new LogCategory( INFOSTREAM_LOGGER_CATEGORY_NAME );

}
