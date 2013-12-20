/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.serialization;

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

import org.hibernate.search.SearchException;

/**
 * Comes from http://www.infoq.com/articles/ApacheAvro
 *
 * @author Boris Lublinsky
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 *         <p/>
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
