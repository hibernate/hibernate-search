/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.logging.impl.LuceneLogCategories;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.util.InfoStream;

/**
 * An implementation of {@link InfoStream}
 * that redirects output to a logger
 */
public class LoggerInfoStream extends InfoStream {

	private static final Log log = LoggerFactory.make( Log.class, LuceneLogCategories.INFOSTREAM_LOGGER_CATEGORY );

	@Override
	public void message(String component, String message) {
		log.logInfoStreamMessage( component, message );
	}

	@Override
	public boolean isEnabled(String component) {
		return log.isTraceEnabled();
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
