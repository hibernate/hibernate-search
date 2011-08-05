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
package org.hibernate.search.remote.codex.avro.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Schema;

import org.hibernate.search.SearchException;
import org.hibernate.search.remote.codex.spi.Deserializer;
import org.hibernate.search.remote.codex.spi.Serializer;
import org.hibernate.search.remote.codex.spi.SerializerProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class AvroSerializerProvider implements SerializerProvider {

	private static final Log log = LoggerFactory.make();
	private Map<String,Schema> schemas;
	private static String V1_PATH = "org/hibernate/search/remote/codex/avro/v1/";
	private int unique;

	@Override
	public Serializer getSerializer() {
		return new AvroSerializer( schemas );
	}

	@Override
	public Deserializer getDeserializer() {
		return new AvroDeserializer( schemas );
	}

	public AvroSerializerProvider() {
		log.debugf( "Use Avro serialization protocol v1" );
		this.schemas = new HashMap<String, Schema>( 20 );
		parseSchema("TermVector");
		parseSchema("Index");
		parseSchema("Store");
		parseSchema("TokenStreamField");
		parseSchema("ReaderField");
		parseSchema("StringField");
		parseSchema("BinaryField");
		parseSchema("NumericIntField");
		parseSchema("NumericLongField");
		parseSchema("NumericFloatField");
		parseSchema("NumericDoubleField");
		parseSchema("CustomFieldable");
		parseSchema("Fieldables");
		parseSchema("Document");
		parseSchema("OptimizeAll");
		parseSchema("PurgeAll");
		parseSchema("Delete");
		parseSchema("Add");
		parseSchema("Operations");
		parseSchema("Message");
	}

	private Schema parseSchema(String filename) {
		String fullFileNameileName = V1_PATH + filename + ".avro";
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( fullFileNameileName );
		String messageSchemaAsString;
		try {
			messageSchemaAsString = readInputStream( in, fullFileNameileName );
		}
		finally {
			try {
				in.close();
			}
			catch ( IOException e ) {
				//we don't care
			}
		}
		String jsonSchema = resolveSchema( messageSchemaAsString );
		Schema schema;
		try {
			schema = Schema.parse( jsonSchema );
		}
		catch ( RuntimeException e ) {
			throw new SearchException( "Unable to parse schema: " +fullFileNameileName + "\n" + jsonSchema, e );
		}
		schemas.put( filename, schema );
		return schema;
	}
	
	public String readInputStream(InputStream inputStream, String filename) {
		try {
			Writer writer = new StringWriter();
			char[] buffer = new char[1000];
			Reader reader = new BufferedReader( new InputStreamReader( inputStream, "UTF-8" ) );
			int r = reader.read( buffer );
			while (r != -1) {
				writer.write( buffer, 0, r );
				r = reader.read( buffer );
			}
			return writer.toString();
		}
		catch ( IOException e ) {
			throw new SearchException( "Unable to read " + filename, e );
		}
	}



	public String resolveSchema(String sc) {

		String result = sc;
		for ( Map.Entry<String, Schema> entry : schemas.entrySet() ) {
			result = replace(
					result, "`" + entry.getKey() + "`",
					entry.getValue().toString()
			);
			//make sure Schema names are unique when embedded
			result = replace(
					result, "__",
					unique + "__" //second level or more inclusion
			);
		}
		unique++;
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
