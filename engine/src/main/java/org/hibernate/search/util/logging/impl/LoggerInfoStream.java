/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

import org.apache.lucene.util.InfoStream;

import java.io.IOException;

/**
 * An implementation of {@link org.apache.lucene.util.InfoStream}
 * that redirects output to a logger
 */
public class LoggerInfoStream extends InfoStream {

	/**
	 * This is the category name of the Logger used to print out the Lucene infostream.
	 * To enable the logger, the category needs to be enabled at TRACE level and configuration
	 * property {@code org.hibernate.search.backend.configuration.impl.IndexWriterSetting#INFOSTREAM}
	 * needs to be enabled on the index.
	 *
	 * @see org.hibernate.search.backend.configuration.impl.IndexWriterSetting#INFOSTREAM
	 */
	public static final String INFOSTREAM_LOGGER_CATEGORY = "org.hibernate.search.backend.lucene.infostream";

	private final Log logger = LoggerFactory.make( INFOSTREAM_LOGGER_CATEGORY );

	@Override
	public void message(String component, String message) {
		logger.logInfoStreamMessage( component, message );
	}

	@Override
	public boolean isEnabled(String component) {
		return logger.isTraceEnabled();
	}

	@Override
	public void close() throws IOException { }
}
