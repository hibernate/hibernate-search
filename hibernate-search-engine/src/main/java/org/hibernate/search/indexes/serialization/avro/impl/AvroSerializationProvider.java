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
package org.hibernate.search.indexes.serialization.avro.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Protocol;

import org.hibernate.search.indexes.serialization.spi.Deserializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.indexes.serialization.spi.Serializer;
import org.hibernate.search.util.impl.FileHelper;
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
	private static String V1_PATH = "org/hibernate/search/remote/codex/avro/v1_0/";
	private static final String AVRO_SCHEMA_FILE_SUFFIX = ".avro";
	private static final String AVRO_PROTOCOL_FILE_SUFFIX = ".avpr";

	private Map<String, String> schemas = new HashMap<String, String>();
	private Protocol protocol;

	public static byte MAJOR_VERSION = (byte) ( -128 + 1 );
	public static byte MINOR_VERSION = (byte) ( -128 + 0 );

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
		parseSchema( "attribute/TokenTrackingAttribute" );
		parseSchema( "attribute/CharTermAttribute" );
		parseSchema( "attribute/PayloadAttribute" );
		parseSchema( "attribute/KeywordAttribute" );
		parseSchema( "attribute/PositionIncrementAttribute" );
		parseSchema( "attribute/FlagsAttribute" );
		parseSchema( "attribute/TypeAttribute" );
		parseSchema( "attribute/OffsetAttribute" );
		parseSchema( "field/TermVector" );
		parseSchema( "field/Index" );
		parseSchema( "field/Store" );
		parseSchema( "field/TokenStreamField" );
		parseSchema( "field/ReaderField" );
		parseSchema( "field/StringField" );
		parseSchema( "field/BinaryField" );
		parseSchema( "field/NumericIntField" );
		parseSchema( "field/NumericLongField" );
		parseSchema( "field/NumericFloatField" );
		parseSchema( "field/NumericDoubleField" );
		parseSchema( "field/CustomFieldable" );
		parseSchema( "Document" );
		parseSchema( "operation/Id" );
		parseSchema( "operation/OptimizeAll" );
		parseSchema( "operation/PurgeAll" );
		parseSchema( "operation/Delete" );
		parseSchema( "operation/Add" );
		parseSchema( "operation/Update" );
		parseSchema( "Message" );

		this.protocol = parseProtocol( "Works" );
	}

	private void parseSchema(String filename) {
		String fullFileName = V1_PATH + filename + AVRO_SCHEMA_FILE_SUFFIX;
		String messageSchemaAsString = FileHelper.readResourceAsString( fullFileName, AvroSerializationProvider.class.getClassLoader() );
		schemas.put( filename, messageSchemaAsString );
	}

	public Protocol parseProtocol(String name) {
		String filename = V1_PATH + name + AVRO_PROTOCOL_FILE_SUFFIX;
		String protocolSkeleton = FileHelper.readResourceAsString( filename, AvroSerializationProvider.class.getClassLoader() );
		String protocolString = inlineSchemas( protocolSkeleton );
		return Protocol.parse( protocolString );
	}

	public String inlineSchemas(String protocolSkeleton) {
		String result = protocolSkeleton;
		for ( Map.Entry<String, String> entry : schemas.entrySet() ) {
			result = replace(
					result, "`" + entry.getKey() + "`",
					entry.getValue()
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
