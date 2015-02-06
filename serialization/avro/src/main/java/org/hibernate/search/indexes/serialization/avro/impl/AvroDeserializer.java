/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.avro.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Protocol;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.util.Utf8;

import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.indexes.serialization.spi.Deserializer;
import org.hibernate.search.indexes.serialization.spi.LuceneWorksBuilder;
import org.hibernate.search.indexes.serialization.spi.SerializableIndex;
import org.hibernate.search.indexes.serialization.spi.SerializableStore;
import org.hibernate.search.indexes.serialization.spi.SerializableTermVector;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Hardy Ferentschik
 */
public class AvroDeserializer implements Deserializer {

	private static final Log log = LoggerFactory.make();
	private final KnownProtocols protocols;
	private List<Utf8> classReferences;

	public AvroDeserializer(KnownProtocols protocols) {
		this.protocols = protocols;
	}

	@Override
	public void deserialize(byte[] data, LuceneWorksBuilder hydrator) {
		final ByteArrayInputStream inputStream = new ByteArrayInputStream( data );
		final int majorVersion = inputStream.read();
		final int minorVersion = inputStream.read();
		final Protocol protocol = protocols.getProtocol( majorVersion, minorVersion );

		Decoder decoder = DecoderFactory.get().binaryDecoder( inputStream, null );
		GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>( protocol.getType( "Message" ) );
		GenericRecord result;
		try {
			result = reader.read( null, decoder );
		}
		catch (IOException e) {
			throw log.unableToDeserializeAvroStream( e );
		}

		classReferences = asListOfString( result, "classReferences" );
		final List<GenericRecord> operations = asListOfGenericRecords( result, "operations" );
		final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		for ( GenericRecord operation : operations ) {
			String schema = operation.getSchema().getName();
			if ( "OptimizeAll".equals( schema ) ) {
				hydrator.addOptimizeAll();
			}
			else if ( "PurgeAll".equals( schema ) ) {
				hydrator.addPurgeAllLuceneWork( asClass( operation, "class" ) );
			}
			else if ( "Flush".equals( schema ) ) {
				hydrator.addFlush();
			}
			else if ( "Delete".equals( schema ) ) {
				processId( operation, hydrator );
				hydrator.addDeleteLuceneWork(
						asClass( operation, "class" ), conversionContext
				);
			}
			else if ( "Add".equals( schema ) ) {
				buildLuceneDocument( asGenericRecord( operation, "document" ), hydrator );
				Map<String, String> analyzers = getAnalyzers( operation );
				processId( operation, hydrator );
				hydrator.addAddLuceneWork(
						asClass( operation, "class" ),
						analyzers,
						conversionContext
				);
			}
			else if ( "Update".equals( schema ) ) {
				buildLuceneDocument( asGenericRecord( operation, "document" ), hydrator );
				Map<String, String> analyzers = getAnalyzers( operation );
				processId( operation, hydrator );
				hydrator.addUpdateLuceneWork(
						asClass( operation, "class" ),
						analyzers,
						conversionContext
				);
			}
			else {
				throw log.cannotDeserializeOperation( schema );
			}
		}
	}

	private String asClass(GenericRecord operation, String attribute) {
		Integer index = (Integer) operation.get( attribute );
		return classReferences.get( index ).toString();
	}

	@SuppressWarnings( "unchecked" )
	private List<Utf8> asListOfString(GenericRecord result, String attribute) {
		return (List<Utf8>) result.get( attribute );
	}

	private void processId(GenericRecord operation, LuceneWorksBuilder hydrator) {
		GenericRecord id = (GenericRecord) operation.get( "id" );
		Object value = id.get( "value" );
		if ( value instanceof ByteBuffer ) {
			hydrator.addIdAsJavaSerialized( asByteArray( (ByteBuffer) value ) );
		}
		else if ( value instanceof Utf8 ) {
			hydrator.addId( value.toString() );
		}
		else {
			//the rest are serialized objects
			hydrator.addId( (Serializable) value );
		}
	}

	private Map<String, String> getAnalyzers(GenericRecord operation) {
		Map<?,?> analyzersWithUtf8 = (Map<?,?>) operation.get( "fieldToAnalyzerMap" );
		if ( analyzersWithUtf8 == null ) {
			return null;
		}
		Map<String,String> analyzers = new HashMap<>( analyzersWithUtf8.size() );
		for ( Map.Entry<?,?> entry : analyzersWithUtf8.entrySet() ) {
			analyzers.put( entry.getKey().toString(), entry.getValue().toString() );
		}
		return analyzers;
	}

	private void buildLuceneDocument(GenericRecord document, LuceneWorksBuilder hydrator) {
		hydrator.defineDocument();
		List<GenericRecord> fieldables = asListOfGenericRecords( document, "fieldables" );
		for ( GenericRecord field : fieldables ) {
			String schema = field.getSchema().getName();
			if ( "CustomFieldable".equals( schema ) ) {
				hydrator.addFieldable( asByteArray( field, "instance" ) );
			}
			else if ( "NumericIntField".equals( schema ) ) {
				hydrator.addIntNumericField(
							asInt( field, "value" ),
							asString( field, "name" ),
							asInt( field, "precisionStep" ),
							asStore( field ),
							asBoolean( field, "indexed" ),
							asFloat( field, "boost" ),
							asBoolean( field, "omitNorms" ),
							asBoolean( field, "omitTermFreqAndPositions" )
				);
			}
			else if ( "NumericFloatField".equals( schema ) ) {
				hydrator.addFloatNumericField(
							asFloat( field, "value" ),
							asString( field, "name" ),
							asInt( field, "precisionStep" ),
							asStore( field ),
							asBoolean( field, "indexed" ),
							asFloat( field, "boost" ),
							asBoolean( field, "omitNorms" ),
							asBoolean( field, "omitTermFreqAndPositions" )
				);
			}
			else if ( "NumericLongField".equals( schema ) ) {
				hydrator.addLongNumericField(
							asLong( field, "value" ),
							asString( field, "name" ),
							asInt( field, "precisionStep" ),
							asStore( field ),
							asBoolean( field, "indexed" ),
							asFloat( field, "boost" ),
							asBoolean( field, "omitNorms" ),
							asBoolean( field, "omitTermFreqAndPositions" )
				);
			}
			else if ( "NumericDoubleField".equals( schema ) ) {
				hydrator.addDoubleNumericField(
							asDouble( field, "value" ),
							asString( field, "name" ),
							asInt( field, "precisionStep" ),
							asStore( field ),
							asBoolean( field, "indexed" ),
							asFloat( field, "boost" ),
							asBoolean( field, "omitNorms" ),
							asBoolean( field, "omitTermFreqAndPositions" )
				);
			}
			else if ( "BinaryField".equals( schema ) ) {
				hydrator.addFieldWithBinaryData(
							asString( field, "name" ),
							asByteArray( field, "value" ),
							asInt( field, "offset" ),
							asInt( field, "length" )
				);
			}
			else if ( "StringField".equals( schema ) ) {
				hydrator.addFieldWithStringData(
						asString( field, "name" ),
						asString( field, "value" ),
						asStore( field ),
						asIndex( field ),
						asTermVector( field ),
						asFloat( field, "boost" ),
						asBoolean( field, "omitNorms" ),
						asBoolean( field, "omitTermFreqAndPositions" )
				);
			}
			else if ( "TokenStreamField".equals( schema ) ) {
				buildAttributes( field, "value", hydrator );
				hydrator.addFieldWithTokenStreamData(
						asString( field, "name" ),
						asTermVector( field ),
						asFloat( field, "boost" ),
						asBoolean( field, "omitNorms" ),
						asBoolean( field, "omitTermFreqAndPositions" )
				);
			}
			else if ( "ReaderField".equals( schema ) ) {
				hydrator.addFieldWithSerializableReaderData(
						asString( field, "name" ),
						asByteArray( field, "value" ),
						asTermVector( field ),
						asFloat( field, "boost" ),
						asBoolean( field, "omitNorms" ),
						asBoolean( field, "omitTermFreqAndPositions" )
				);
			}
			else if ( "BinaryDocValuesField".equals( schema ) ) {
				hydrator.addDocValuesFieldWithBinaryData(
						asString( field, "name" ),
						asString( field, "type" ),
						asByteArray( field, "value" ),
						asInt( field, "offset" ),
						asInt( field, "length" )
				);
			}
			else if ( "NumericDocValuesField".equals( schema ) ) {
				hydrator.addDocValuesFieldWithNumericData(
						asString( field, "name" ),
						asString( field, "type" ),
						asLong( field, "value" )
				);
			}
			else {
				throw log.cannotDeserializeField( schema );
			}
		}
	}

	private void buildAttributes(GenericRecord record, String field, LuceneWorksBuilder hydrator) {
		@SuppressWarnings( "unchecked" )
		List<List<?>> tokens = (List<List<?>>) record.get( field );
		for ( List<?> token : tokens ) {
			for ( Object attribute : token ) {
				buildAttribute( attribute, hydrator );
			}
			hydrator.addToken();
		}
	}

	private void buildAttribute(Object element, LuceneWorksBuilder hydrator) {
		if ( element instanceof GenericRecord ) {
			GenericRecord record = (GenericRecord) element;
			String name = record.getSchema().getName();
			if ( "TokenTrackingAttribute".equals( name ) ) {
				@SuppressWarnings( "unchecked" )
				List<Integer> positionList = (List<Integer>) record.get( "positions" );
				hydrator.addTokenTrackingAttribute( positionList );
			}
			else if ( "CharTermAttribute".equals( name ) ) {
				hydrator.addCharTermAttribute( (CharSequence) record.get( "sequence" ) );
			}
			else if ( "PayloadAttribute".equals( name ) ) {
				hydrator.addPayloadAttribute( asByteArray( record, "payload") );
			}
			else if ( "KeywordAttribute".equals( name ) ) {
				hydrator.addKeywordAttribute( asBoolean( record, "isKeyword") );
			}
			else if ( "PositionIncrementAttribute".equals( name ) ) {
				hydrator.addPositionIncrementAttribute( asInt( record, "positionIncrement") );
			}
			else if ( "FlagsAttribute".equals( name ) ) {
				hydrator.addFlagsAttribute( asInt( record, "flags") );
			}
			else if ( "TypeAttribute".equals( name ) ) {
				hydrator.addTypeAttribute( asString( record, "type") );
			}
			else if ( "OffsetAttribute".equals( name ) ) {
				hydrator.addOffsetAttribute( asInt( record, "startOffset"), asInt( record, "endOffset" ) );
			}
			else {
				throw log.unknownAttributeSerializedRepresentation( name );
			}
		}
		else if ( element instanceof ByteBuffer ) {
			hydrator.addSerializedAttribute( asByteArray( (ByteBuffer) element ) );
		}
		else {
			throw log.unknownAttributeSerializedRepresentation( element.getClass().getName() );
		}
	}

	private GenericRecord asGenericRecord(GenericRecord operation, String field) {
		return (GenericRecord) operation.get( field );
	}

	@SuppressWarnings( "unchecked" )
	private List<GenericRecord> asListOfGenericRecords(GenericRecord result, String field) {
		return (List<GenericRecord>) result.get( field );
	}

	private float asFloat(GenericRecord record, String field) {
		return ( (Float) record.get( field ) ).floatValue();
	}

	private int asInt(GenericRecord record, String field) {
		return ( (Integer) record.get( field ) ).intValue();
	}

	private long asLong(GenericRecord record, String field) {
		return ( (Long) record.get( field ) ).longValue();
	}

	private double asDouble(GenericRecord record, String field) {
		return ( (Double) record.get( field ) ).doubleValue();
	}

	private String asString(GenericRecord record, String field) {
		return record.get( field ).toString();
	}

	private boolean asBoolean(GenericRecord record, String field) {
		return ( (Boolean) record.get( field ) ).booleanValue();
	}

	private SerializableStore asStore(GenericRecord field) {
		String string = field.get( "store" ).toString();
		return SerializableStore.valueOf( string );
	}

	private SerializableIndex asIndex(GenericRecord field) {
		String string = field.get( "index" ).toString();
		return SerializableIndex.valueOf( string );
	}

	private SerializableTermVector asTermVector(GenericRecord field) {
		String string = field.get( "termVector" ).toString();
		return SerializableTermVector.valueOf( string );
	}

	private byte[] asByteArray(GenericRecord operation, String field) {
		ByteBuffer buffer = (ByteBuffer) operation.get( field );
		return asByteArray( buffer );
	}

	private byte[] asByteArray(ByteBuffer buffer) {
		byte[] copy = new byte[buffer.remaining()];
		buffer.get( copy );
		return copy;
	}
}
