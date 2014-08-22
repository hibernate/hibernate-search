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

	private final Log logger = LoggerFactory.make();

	@Override
	public void message(String component, String message) {
		logger.logInfoStreamMessage( component, message );
	}

	@Override
	public boolean isEnabled(String component) {
		return true;
	}

	@Override
	public void close() throws IOException { }
}
