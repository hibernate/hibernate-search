/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import org.hibernate.search.backend.lucene.logging.impl.LuceneInfoStreamLog;

import org.apache.lucene.util.InfoStream;

/**
 * An implementation of {@link InfoStream}
 * that redirects output to a logger
 */
public class LoggerInfoStream extends InfoStream {

	@Override
	public void message(String component, String message) {
		LuceneInfoStreamLog.INSTANCE.logInfoStreamMessage( component, message );
	}

	@Override
	public boolean isEnabled(String component) {
		return LuceneInfoStreamLog.INSTANCE.isTraceEnabled();
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
