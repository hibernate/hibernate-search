/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Hardy Ferentschik
 */
public final class LoggerHelper {

	private static final Log log = LoggerFactory.make();

	private LoggerHelper() {
		//now allowed
	}

	public static PrintStream getLoggingPrintStream() {
		return new PrintStream( new CustomByteArrayOutputStream(), true );
	}

	static class CustomByteArrayOutputStream extends ByteArrayOutputStream {
		@Override
		public void flush() throws IOException {
			log.debug( this.toString() );
			super.flush();
		}
	}
}
