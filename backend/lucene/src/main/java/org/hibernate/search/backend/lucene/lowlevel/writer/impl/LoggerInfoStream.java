/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	private final Log logger = LoggerFactory.make( Log.class, LuceneLogCategories.INFOSTREAM_LOGGER_CATEGORY );

	@Override
	public void message(String component, String message) {
		logger.logInfoStreamMessage( component, message );
	}

	@Override
	public boolean isEnabled(String component) {
		return logger.isTraceEnabled();
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
