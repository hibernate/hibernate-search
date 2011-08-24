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
package org.hibernate.search.indexes.serialization.codex.avro.impl;

import java.io.BufferedReader;
import java.io.File;
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
import org.hibernate.search.indexes.serialization.codex.spi.Deserializer;
import org.hibernate.search.indexes.serialization.codex.spi.SerializationProvider;
import org.hibernate.search.indexes.serialization.codex.spi.Serializer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * parsing code inspired by http://www.infoq.com/articles/ApacheAvro
 * from Boris Lublinsky
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class AvroSerializationProvider implements SerializationProvider {

	private static final Log log = LoggerFactory.make();
	private Map<String, String> schemas = new HashMap<String,String>();
	private static String V1_PATH = "org/hibernate/search/remote/codex/avro/v1/";
	public static byte MAJOR_VERSION = ( byte ) ( -128 + 1 );
	public static byte MINOR_VERSION = ( byte ) ( -128 + 0 );
	private int unique;
	private Protocol protocol;

	public static int getMajorVersion() {
		return MAJOR_VERSION + 128; //rebase to 0
	}

	public static int getMinorVersion() {
		return MINOR_VERSION + 128; //rebase to 0
	}

	@Override
	public Serializer getSerializer() {
		return new AvroSerializer( protocol );
	}

	@Override
	public Deserializer getDeserializer() {
		return new AvroDeserializer( protocol );
	}

	public AvroSerializationProvider() {
		log.serializationProtocol( getMajorVersion(), getMinorVersion() );
		parseSchema( "TermVector" );
		parseSchema( "Index" );
		parseSchema( "Store" );
		parseSchema( "attribute/TokenTrackingAttribute" );
		parseSchema( "attribute/CharTermAttribute" );
		parseSchema( "attribute/PayloadAttribute" );
		parseSchema( "attribute/KeywordAttribute" );
		parseSchema( "attribute/PositionIncrementAttribute" );
		parseSchema( "attribute/FlagsAttribute" );
		parseSchema( "attribute/TypeAttribute" );
		parseSchema( "attribute/OffsetAttribute" );
		parseSchema( "TokenStreamField" );
		parseSchema( "ReaderField" );
		parseSchema( "StringField" );
		parseSchema( "BinaryField" );
		parseSchema( "NumericIntField" );
		parseSchema( "NumericLongField" );
		parseSchema( "NumericFloatField" );
		parseSchema( "NumericDoubleField" );
		parseSchema( "CustomFieldable" );
		parseSchema( "Document" );
		parseSchema( "OptimizeAll" );
		parseSchema( "PurgeAll" );
		parseSchema( "Delete" );
		parseSchema( "Add" );
		parseSchema( "Update" );
		parseSchema( "Message" );

		this.protocol = parseProtocol( "Works" );
	}

	private void parseSchema(String filename) {
		String fullFileName = V1_PATH + filename + ".avro";
		fullFileName = fullFileName.replace( '/', File.separatorChar );
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( fullFileName );
		String messageSchemaAsString;
		try {
			messageSchemaAsString = readInputStream( in, fullFileName );
		}
		finally {
			try {
				in.close();
			}
			catch ( IOException e ) {
				//we don't care
			}
		}
		schemas.put( filename, messageSchemaAsString );
	}

	public String readInputStream(InputStream inputStream, String filename) {
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
		catch ( IOException e ) {
			throw new SearchException( "Unable to read " + filename, e );
		}
	}

	public Protocol parseProtocol(String name) {
		String filename = V1_PATH + name + ".avpr";
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( filename );
		String protocolSkeleton = readInputStream( in, filename );
		String protocolString = inlineSchemas( protocolSkeleton );
		return Protocol.parse( protocolString );
	}

	public String inlineSchemas(String protocolSkeleton) {
		String result = protocolSkeleton;
		for ( Map.Entry<String, String> entry : schemas.entrySet() ) {
			result = replace(
					result, "`" + entry.getKey() + "`",
					entry.getValue().toString()
			);
		}
		return result;
	}

	static String replace(String str, String pattern, String replace) {
		int s = 0;
		int e;
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
