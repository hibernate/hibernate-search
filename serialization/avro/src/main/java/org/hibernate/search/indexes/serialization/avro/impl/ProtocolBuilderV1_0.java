/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.avro.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Protocol;
import org.hibernate.search.indexes.serialization.avro.logging.impl.Log;
import org.hibernate.search.util.impl.StreamHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper to build an Avro Protocol version 1.0 from all our resource
 * schemas.
 */
class ProtocolBuilderV1_0 {

	private static final String AVRO_SCHEMA_FILE_SUFFIX = ".avro";
	private static final String AVRO_PROTOCOL_FILE_SUFFIX = ".avpr";
	private static String V1_0_PATH = "org/hibernate/search/remote/codex/avro/v1_0/";
	private static final Log log = LoggerFactory.make( Log.class );

	private final Map<String, String> schemas = new HashMap<String, String>();

	/**
	 * @return an Avro Protocol at version 1.0
	 */
	Protocol build() {
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
		return parseProtocol( "Works" );
	}

	protected final void parseSchema(String filename) {
		String fullFileName = getResourceBasePath() + filename + AVRO_SCHEMA_FILE_SUFFIX;
		String schema = avroResourceAsString( fullFileName );
		schemas.put( filename, schema );
	}

	protected String getResourceBasePath() {
		return V1_0_PATH;
	}

	protected final Protocol parseProtocol(String name) {
		String fullFileName = getResourceBasePath() + name + AVRO_PROTOCOL_FILE_SUFFIX;
		String protocolSkeleton = avroResourceAsString( fullFileName );
		String protocol = inlineSchemas( protocolSkeleton );
		return Protocol.parse( protocol );
	}

	private String inlineSchemas(String protocolSkeleton) {
		String result = protocolSkeleton;
		for ( Map.Entry<String, String> entry : schemas.entrySet() ) {
			result = replace(
					result, "`" + entry.getKey() + "`",
					entry.getValue()
			);
		}
		return result;
	}

	private static String replace(String str, String pattern, String replace) {
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

	private static String avroResourceAsString(String resourceName) {
		// using class loader of AvroSerializationProvider, because we load resources included in the same artifact.
		InputStream inputStream = AvroSerializationProvider.class.getClassLoader().getResourceAsStream( resourceName );
		if ( inputStream == null ) {
			throw log.unableToLoadAvroSchema( resourceName );
		}

		String resource;
		try {
			resource = StreamHelper.readInputStream( inputStream );
		}
		catch (IOException e) {
			throw log.unableToLoadResource( resourceName );
		}
		finally {
			StreamHelper.closeResource( inputStream );
		}
		return resource;
	}

}
