/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	 *
	 * @return the content of the input stream as string
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


