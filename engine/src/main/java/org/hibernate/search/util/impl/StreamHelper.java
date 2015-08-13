/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Hardy Ferentschik
 */
public class StreamHelper {

	private static final Log log = LoggerFactory.make();

	private StreamHelper() {
	}

	/**
	 * Reads the provided input stream into a string
	 *
	 * @param inputStream the input stream to read from
	 * @return the content of the input stream as string
	 * @throws IOException if an I/O error occurs while reading the {@link InputStream}
	 */
	public static String readInputStream(InputStream inputStream) throws IOException {
		Writer writer = new StringWriter();
		try {
			char[] buffer = new char[1000];
			Reader reader = new BufferedReader( new InputStreamReader( inputStream, "UTF-8" ) );
			int r = reader.read( buffer );
			while ( r != -1 ) {
				writer.write( buffer, 0, r );
				r = reader.read( buffer );
			}
			return writer.toString();
		}
		finally {
			closeResource( writer );
		}
	}

	/**
	 * Closes a resource without throwing IOExceptions
	 *
	 * @param resource the resource to close
	 */
	public static void closeResource(Closeable resource) {
		if ( resource != null ) {
			try {
				resource.close();
			}
			catch (IOException e) {
				//we don't really care if we can't close
				log.couldNotCloseResource( e );
			}
		}
	}
}


