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

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.search.test.serialization.AvroUtils.parseProtocol;
import static org.hibernate.search.test.serialization.AvroUtils.parseSchema;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.util.Utf8;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class AvroTest {


	@Test
	public void experimentWithAvro() throws Exception {
		String root = "org/hibernate/search/remote/codex/avro/v1_0/";
		parseSchema( root + "attribute/TokenTrackingAttribute.avro", "attribute/TokenTrackingAttribute" );
		parseSchema( root + "attribute/CharTermAttribute.avro", "attribute/CharTermAttribute" );
		parseSchema( root + "attribute/PayloadAttribute.avro", "attribute/PayloadAttribute" );
		parseSchema( root + "attribute/KeywordAttribute.avro", "attribute/KeywordAttribute" );
		parseSchema( root + "attribute/PositionIncrementAttribute.avro", "attribute/PositionIncrementAttribute" );
		parseSchema( root + "attribute/FlagsAttribute.avro", "attribute/FlagsAttribute" );
		parseSchema( root + "attribute/TypeAttribute.avro", "attribute/TypeAttribute" );
		parseSchema( root + "attribute/OffsetAttribute.avro", "attribute/OffsetAttribute" );
		parseSchema( root + "field/TermVector.avro", "field/TermVector" );
		parseSchema( root + "field/Index.avro", "field/Index" );
		parseSchema( root + "field/Store.avro", "field/Store" );
		parseSchema( root + "field/TokenStreamField.avro", "field/TokenStreamField" );
		parseSchema( root + "field/ReaderField.avro", "field/ReaderField" );
		parseSchema( root + "field/StringField.avro", "field/StringField" );
		parseSchema( root + "field/BinaryField.avro", "field/BinaryField" );
		parseSchema( root + "field/NumericIntField.avro", "field/NumericIntField" );
		parseSchema( root + "field/NumericLongField.avro", "field/NumericLongField" );
		parseSchema( root + "field/NumericFloatField.avro", "field/NumericFloatField" );
		parseSchema( root + "field/NumericDoubleField.avro", "field/NumericDoubleField" );
		parseSchema( root + "field/CustomFieldable.avro", "field/CustomFieldable" );
		parseSchema( root + "Document.avro", "Document" );
		parseSchema( root + "operation/Id.avro", "operation/Id" );
		parseSchema( root + "operation/OptimizeAll.avro", "operation/OptimizeAll" );
		parseSchema( root + "operation/PurgeAll.avro", "operation/PurgeAll" );
		parseSchema( root + "operation/Delete.avro", "operation/Delete" );
		parseSchema( root + "operation/Add.avro", "operation/Add" );
		parseSchema( root + "operation/Update.avro", "operation/Update" );
		parseSchema( root + "Message.avro", "Message" );


		String filename = root + "Works.avpr";
		Protocol protocol = parseProtocol( filename, "Works" );
		final Schema termVectorSchema = protocol.getType( "TermVector" );
		final Schema indexSchema = protocol.getType( "Index" );
		final Schema storeSchema = protocol.getType( "Store" );
		final Schema tokenTrackingAttribute = protocol.getType( "TokenTrackingAttribute" );
		final Schema tokenStreamSchema = protocol.getType( "TokenStreamField" );
		final Schema readerSchema = protocol.getType( "ReaderField" );
		final Schema stringSchema = protocol.getType( "StringField" );
		final Schema binarySchema = protocol.getType( "BinaryField" );
		final Schema intFieldSchema = protocol.getType( "NumericIntField" );
		final Schema longFieldSchema = protocol.getType( "NumericLongField" );
		final Schema floatFieldSchema = protocol.getType( "NumericFloatField" );
		final Schema doubleFieldSchema = protocol.getType( "NumericDoubleField" );
		final Schema custonFieldableSchema = protocol.getType( "CustomFieldable" );
		final Schema documentSchema = protocol.getType( "Document" );
		final Schema idSchema = protocol.getType( "Id" );
		final Schema optimizeAllSchema = protocol.getType( "OptimizeAll" );
		final Schema purgeAllSchema = protocol.getType( "PurgeAll" );
		final Schema deleteSchema = protocol.getType( "Delete" );
		final Schema addSchema = protocol.getType( "Add" );
		final Schema updateSchema = protocol.getType( "Update" );
		Schema messageSchema = protocol.getType( "Message" );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>( messageSchema );
		Encoder encoder = EncoderFactory.get().directBinaryEncoder( out, null );

		byte[] serializableSample = new byte[10];
		for ( int i = 0; i < 10; i++ ) {
			serializableSample[i] = (byte) i;
		}

		List<String> classReferences = new ArrayList<String>();
		classReferences.add( AvroTest.class.getName() );

		List<GenericRecord> fieldables = new ArrayList<GenericRecord>( 1 );
		//custom fieldable
		GenericRecord customFieldable = new GenericData.Record( custonFieldableSchema );
		customFieldable.put( "instance", ByteBuffer.wrap( serializableSample ) );
		fieldables.add( customFieldable );

		//numeric fields
		GenericRecord numericField = createNumeric( intFieldSchema );
		numericField.put( "value", 3 );
		fieldables.add( numericField );
		numericField = createNumeric( longFieldSchema );
		numericField.put( "value", 3l );
		fieldables.add( numericField );
		numericField = createNumeric( floatFieldSchema );
		numericField.put( "value", 2.3f );
		fieldables.add( numericField );
		numericField = createNumeric( doubleFieldSchema );
		numericField.put( "value", 2.3d );
		fieldables.add( numericField );

		//fields
		GenericRecord field = createField( binarySchema );
		field.put( "offset", 0 );
		field.put( "length", 10 );
		field.put( "value", ByteBuffer.wrap( serializableSample ) );
		fieldables.add( field );
		field = createField( stringSchema );
		field.put( "value", stringSchema.getName() );
		field.put( "store", "YES" );
		field.put( "index", "ANALYZED" );
		field.put( "termVector", "WITH_OFFSETS" );
		fieldables.add( field );
		field = createField( tokenStreamSchema );

		List<List<Object>> tokens = new ArrayList<List<Object>>();
		List<Object> attrs = new ArrayList<Object>();
		tokens.add( attrs );
		GenericData.Record attr = new GenericData.Record( tokenTrackingAttribute );
		List<Integer> positions = new ArrayList<Integer>();
		positions.add( 1 );
		positions.add( 2 );
		positions.add( 3 );
		positions.add( 4 );
		attr.put( "positions", positions);
		attrs.add( attr );
		attrs.add( ByteBuffer.wrap( serializableSample ) );

		field.put( "value", tokens );
		field.put( "termVector", "WITH_OFFSETS" );
		fieldables.add( field );
		field = createField( readerSchema );
		field.put( "value", ByteBuffer.wrap( serializableSample ) );
		field.put( "termVector", "WITH_OFFSETS" );
		fieldables.add( field );

		GenericRecord doc = new GenericData.Record( documentSchema );
		doc.put( "boost", 2.3f );
		doc.put( "fieldables", fieldables );

		GenericRecord add = new GenericData.Record( addSchema );
		add.put( "class", classReferences.indexOf( AvroTest.class.getName() ) );
		GenericRecord id = new GenericData.Record( idSchema );
		id.put( "value", ByteBuffer.wrap( serializableSample ) );
		add.put( "id", id );
		add.put( "document", doc );
		Map<String, String> analyzers = new HashMap<String, String>();
		analyzers.put( "name", "ngram" );
		analyzers.put( "description", "porter" );
		add.put( "fieldToAnalyzerMap", analyzers );

		GenericRecord delete = new GenericData.Record( deleteSchema );
		delete.put( "class", classReferences.indexOf( AvroTest.class.getName() ) );
		id = new GenericData.Record( idSchema );
		id.put( "value", new Long(30) );
		delete.put( "id", id );

		GenericRecord purgeAll = new GenericData.Record( purgeAllSchema );
		purgeAll.put( "class", classReferences.indexOf( AvroTest.class.getName() ) );
		GenericRecord optimizeAll = new GenericData.Record( optimizeAllSchema );

		List<GenericRecord> operations = new ArrayList<GenericRecord>( 1 );
		operations.add( purgeAll );
		operations.add( optimizeAll );
		operations.add( delete );
		operations.add( add );


		GenericRecord message = new GenericData.Record( messageSchema );
		message.put( "classReferences", classReferences );
		message.put( "operations", operations );

		writer.write( message, encoder );
		encoder.flush();

		ByteArrayInputStream inputStream = new ByteArrayInputStream( out.toByteArray() );
		Decoder decoder = DecoderFactory.get().binaryDecoder( inputStream, null );
		GenericDatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>( messageSchema );
		while ( true ) {
			try {
				GenericRecord result = reader.read( null, decoder );
				System.out.println( result );

				assertThat( result ).isNotNull();
				//operations
				assertThat( result.get( "operations" ) ).isNotNull().isInstanceOf( List.class );
				List<?> ops = (List<?>) result.get( "operations" );
				assertThat( ops ).hasSize( 4 );

				//Delete
				assertThat( ops.get( 2 ) ).isInstanceOf( GenericRecord.class );
				GenericRecord deleteOp = (GenericRecord) ops.get( 2 );
				assertThat( deleteOp.getSchema().getName() ).isEqualTo( "Delete" );
				Object actual = ( (GenericRecord) deleteOp.get( "id" ) ).get( "value" );
				assertThat( actual ).isInstanceOf( Long.class );
				assertThat( actual ).isEqualTo( Long.valueOf( 30 ) );

				//Add
				assertThat( ops.get( 3 ) ).isInstanceOf( GenericRecord.class );
				GenericRecord addOp = (GenericRecord) ops.get( 3 );
				assertThat( addOp.getSchema().getName() ).isEqualTo( "Add" );
				actual = ( (GenericRecord) addOp.get( "id" ) ).get( "value" );
				assertThat( actual ).isInstanceOf( ByteBuffer.class );
				ByteBuffer bb = (ByteBuffer) actual;
				assertThat( bb.hasArray() ).isTrue();
				byte[] copy = new byte[bb.remaining()];
				bb.get( copy );
				assertThat( serializableSample ).isEqualTo( copy );

				//fieldToAnalyzerMap
				assertThat( addOp.get( "fieldToAnalyzerMap" ) ).isInstanceOf( Map.class );
				assertThat( (Map) addOp.get( "fieldToAnalyzerMap" ) ).hasSize( 2 );

				//document
				assertThat( addOp.get( "document" ) ).isNotNull();
				GenericRecord document = (GenericRecord) addOp.get( "document" );
				assertThat( document.get( "boost" ) ).isEqualTo( 2.3f );

				//numeric fields
				assertThat( document.get( "fieldables" ) ).isNotNull().isInstanceOf( List.class );
				List<?> fields = (List<?>) document.get( "fieldables" );

				assertThat( fields ).hasSize( 9 ); //custom + 4 numerics + 4 fields

				field = (GenericRecord) fields.get( 0 );
				assertThat( field.getSchema().getName() ).isEqualTo( "CustomFieldable" );
				field = (GenericRecord) fields.get( 1 );
				assertThat( field.getSchema().getName() ).isEqualTo( "NumericIntField" );
				assertThat( field.get( "value" ) ).isEqualTo( 3 );
				assertNumericField( field );
				field = (GenericRecord) fields.get( 2 );
				assertThat( field.getSchema().getName() ).isEqualTo( "NumericLongField" );
				assertThat( field.get( "value" ) ).isEqualTo( 3l );
				assertNumericField( field );
				field = (GenericRecord) fields.get( 3 );
				assertThat( field.getSchema().getName() ).isEqualTo( "NumericFloatField" );
				assertThat( field.get( "value" ) ).isEqualTo( 2.3f );
				assertNumericField( field );
				field = (GenericRecord) fields.get( 4 );
				assertThat( field.getSchema().getName() ).isEqualTo( "NumericDoubleField" );
				assertThat( field.get( "value" ) ).isEqualTo( 2.3d );
				assertNumericField( field );

				//fields
				field = (GenericRecord) fields.get( 5 );
				assertThat( field.getSchema().getName() ).isEqualTo( "BinaryField" );
				assertThat( field.get( "value" ) ).isInstanceOf( ByteBuffer.class );
				assertField( field );

				field = (GenericRecord) fields.get( 6 );
				assertThat( field.getSchema().getName() ).isEqualTo( "StringField" );
				assertThat( field.get( "value" ) ).isInstanceOf( Utf8.class );
				assertTermVector( field );
				assertIndexAndStore( field );
				assertField( field );

				field = (GenericRecord) fields.get( 7 );
				assertThat( field.getSchema().getName() ).isEqualTo( "TokenStreamField" );
				assertThat( field.get( "value" ) ).isInstanceOf( List.class );
				List<List<Object>> l1 = (List<List<Object>>) field.get( "value" );
				assertThat( l1.get( 0 ) ).as( "Wrong attribute impl list" ).hasSize( 2 );
				Object object = l1.get( 0 ).get( 0 );
				assertThat( object ).isNotNull();
				assertTermVector( field );
				assertField( field );

				field = (GenericRecord) fields.get( 8 );
				assertThat( field.getSchema().getName() ).isEqualTo( "ReaderField" );
				assertThat( field.get( "value" ) ).isInstanceOf( ByteBuffer.class );
				assertTermVector( field );
				assertField( field );
			}
			catch (EOFException eof) {
				break;
			}
			catch (Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	private void assertTermVector(GenericRecord field) {
		assertThat( field.get( "termVector" ) ).isInstanceOf( GenericEnumSymbol.class );
		assertThat( field.get( "termVector" ).toString() ).isEqualTo( "WITH_OFFSETS" );
	}

	private void assertIndexAndStore(GenericRecord field) {
		assertThat( field.get( "index" ) ).isInstanceOf( GenericEnumSymbol.class );
		assertThat( field.get( "index" ).toString() ).isEqualTo( "ANALYZED" );
		assertThat( field.get( "store" ) ).isInstanceOf( GenericEnumSymbol.class );
		assertThat( field.get( "store" ).toString() ).isEqualTo( "YES" );
	}

	private void assertField(GenericRecord field) {
		assertThat( field.get( "name" ) ).isInstanceOf( Utf8.class );
		assertThat( field.get( "name" ).toString() ).isEqualTo( field.getSchema().getName() );
		assertThat( field.get( "boost" ) ).isEqualTo( 2.3f );
		assertThat( field.get( "omitNorms" ) ).isEqualTo( true );
		assertThat( field.get( "omitTermFreqAndPositions" ) ).isEqualTo( true );
	}

	private GenericRecord createField(Schema schema) {
		GenericRecord field = new GenericData.Record( schema );
		field.put( "name", schema.getName() );
		field.put( "boost", 2.3f );
		field.put( "omitNorms", true );
		field.put( "omitTermFreqAndPositions", true );
		return field;
	}

	private void assertNumericField(GenericRecord field) {
		assertThat( field.get( "name" ) ).isInstanceOf( Utf8.class );
		assertThat( field.get( "name" ).toString() ).isEqualTo( "int" );
		assertThat( field.get( "precisionStep" ) ).isEqualTo( 3 );
		assertThat( field.get( "boost" ) ).isEqualTo( 2.3f );
		assertThat( field.get( "indexed" ) ).isEqualTo( true );
		assertThat( field.get( "omitNorms" ) ).isEqualTo( true );
		assertThat( field.get( "omitTermFreqAndPositions" ) ).isEqualTo( true );
		assertThat( ( field.get( "store" ) ) ).isInstanceOf( GenericData.EnumSymbol.class );
		assertThat( ( field.get( "store" ) ).toString() ).isEqualTo( "YES" );
	}

	private GenericRecord createNumeric(Schema schema) {
		GenericRecord numericField = new GenericData.Record( schema );
		numericField.put( "name", "int" );
		numericField.put( "precisionStep", 3 );
		numericField.put( "store", "YES" );
		numericField.put( "indexed", true );
		numericField.put( "boost", 2.3f );
		numericField.put( "omitNorms", true );
		numericField.put( "omitTermFreqAndPositions", true );
		return numericField;
	}
}
