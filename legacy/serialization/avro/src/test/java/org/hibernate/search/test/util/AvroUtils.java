/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Protocol;

import org.hibernate.search.exception.SearchException;

/**
 * Comes from http://www.infoq.com/articles/ApacheAvro
 *
 * @author Boris Lublinsky
 * @author Emmanuel Bernard
 *         <p>
 *         //TODO convert to non static method
 */
public class AvroUtils {

	private static Map<String, String> schemas = new HashMap<String, String>();

	private AvroUtils() {
	}

	public static void parseSchema(String filename, String name) {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( filename );
		String messageSchemaAsString = AvroUtils.readInputStream( in, filename );
		AvroUtils.addSchema( name, messageSchemaAsString );
	}

	public static String readInputStream(InputStream inputStream, String filename) {
		try {
			Writer writer = new StringWriter();
			char[] buffer = new char[1000];
			Reader reader = new BufferedReader( new InputStreamReader( inputStream, "UTF-8" ) );
			int r = reader.read( buffer );
			while ( r != -1 ) {
				writer.write( buffer, 0, r );
				r = reader.read( buffer );
			}
			return writer.toString();
		}
		catch (IOException e) {
			throw new SearchException( "Unable to read " + filename, e );
		}
	}

	public static void addSchema(String name, String schema) {
		schemas.put( "`" + name + "`", schema );

	}

	public static Protocol parseProtocol(String filename, String name) {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( filename );
		String protocolSkeleton = AvroUtils.readInputStream( in, filename );
		String protocolString = inlineSchemas( protocolSkeleton );
		//System.out.println("\n\n" + protocolString + "\n\n");
		return Protocol.parse( protocolString );
	}

	public static String inlineSchemas(String protocolSkeleton) {
		String result = protocolSkeleton;
		for ( Map.Entry<String, String> entry : schemas.entrySet() ) {
			result = replace(
					result, entry.getKey(),
					entry.getValue().toString()
			);
		}
		return result;

	}

	static String replace(String str, String pattern, String replace) {

		int s = 0;
		int e = 0;
		StringBuilder result = new StringBuilder();
		while ( ( e = str.indexOf( pattern, s ) ) >= 0 ) {
			result.append( str.substring( s, e ) );
			result.append( replace );
			s = e + pattern.length();

		}
		result.append( str.substring( s ) );
		return result.toString();
	}

}
