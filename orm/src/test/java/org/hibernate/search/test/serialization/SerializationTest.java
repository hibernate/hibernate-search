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

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.FlagsAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttributeImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.Payload;
import org.apache.lucene.util.AttributeImpl;
import org.apache.solr.handler.AnalysisRequestHandlerBase;
import org.junit.Test;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.indexes.serialization.avro.impl.AvroSerializationProvider;
import org.hibernate.search.indexes.serialization.impl.CopyTokenStream;
import org.hibernate.search.indexes.serialization.impl.PluggableSerializationLuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.impl.SerializationHelper;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializableTokenStream;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SerializationTest extends SearchTestCase {
	private static final Log log = LoggerFactory.make();

	@Test
	public void testAvroSerialization() throws Exception {
		LuceneWorkSerializer converter = new PluggableSerializationLuceneWorkSerializer(
				new AvroSerializationProvider(),
				getSearchFactoryImpl()
		);
		List<LuceneWork> works = buildWorks();

		byte[] bytes = converter.toSerializedModel( works );
		List<LuceneWork> copyOfWorks = converter.toLuceneWorks( bytes );

		assertThat( copyOfWorks ).hasSize( works.size() );
		for ( int index = 0; index < works.size(); index++ ) {
			assertLuceneWork( works.get( index ), copyOfWorks.get( index ) );
		}
	}

	@Test
	/**
	 * 20110815
	 * Our avro serializer is slower (1.6) than Java serialization esp when the VM is not warm (small loop value like = 1000
	 * In evens up on longer loops like 100000
	 *
	 * Our avro deserializer is slower (2.5) than Java serialization esp when the VM is not warm (small loop value like = 1000
	 * In evens up or beats the Java serialization on longer loops like 100000
	 *
	 * Test done after initial implementation (in particular the schema is not part of the message
	 *
	 * With 1000000:
	 * Java serialization: 28730
	 * Java message size: 2509
	 * Java deserialization: 82970
	 * Avro serialization: 24245
	 * Avro message size: 1064
	 * Avro deserialization: 54444
	 *
	 *
	 * 20110824
	 * The new Work sample is bigger and Avro's layer has been optimized
	 * Our avro serializer is faster (1.8 times) than Java serialization for 100000.
	 *
	 * Our avro deserializer is faster (2.7 times) than Java serialization for 100000.
	 *
	 * The message size is 4.4 times smaller in Avro
	 *
	 * (the schema is not part of the message)
	 *
	 * With 1000000:
	 * Java serialization: 55786
	 * Java message size: 4094
	 * Java deserialization: 160764
	 * Avro serialization: 30430
	 * Avro message size: 929
	 * Avro deserialization: 59255
	 *
	 * 20110826
	 * Our avro serializer is faster (1.7 times) than Java serialization for 100000.
	 *
	 * Our avro deserializer is faster (2.7 times) than Java serialization for 100000.
	 *
	 * The message size is 6.6 times smaller in Avro
	 *
	 * (the schema is not part of the message)
	 *
	 * With 1000000:
	 * Java serialization: 52682
	 * Java message size: 4094
	 * Java de-serialization: 168595
	 * Avro serialization: 30586
	 * Avro message size: 617
	 * Avro deserialization: 62141
	 */
	public void testAvroSerializationPerf() throws Exception {
		final int loop = 10; //TODO do 10000 or 100000
		LuceneWorkSerializer converter = new PluggableSerializationLuceneWorkSerializer(
				new AvroSerializationProvider(),
				getSearchFactoryImpl()
		);
		List<LuceneWork> works = buildWorks();

		long begin;
		long end;
		byte[] javaBytes = null;
		begin = System.nanoTime();
		for ( int i = 0; i < loop; i++ ) {
			javaBytes = SerializationHelper.toByteArray( (Serializable) works );
		}
		end = System.nanoTime();
		log.debug( "Java serialization: " + ( ( end - begin ) / 1000000 ) );
		log.debug( "Java message size: " + javaBytes.length );

		begin = System.nanoTime();

		List<LuceneWork> copyOfWorkForJavaSerial = null;
		for ( int i = 0; i < loop; i++ ) {
			copyOfWorkForJavaSerial = (List<LuceneWork>) SerializationHelper.toSerializable(
					javaBytes,
					Thread.currentThread().getContextClassLoader()
			);
		}
		end = System.nanoTime();
		log.debug( "Java de-serialization: " + ( ( end - begin ) / 1000000 ) );

		byte[] avroBytes = null;
		begin = System.nanoTime();
		for ( int i = 0; i < loop; i++ ) {
			avroBytes = converter.toSerializedModel( works );
		}
		end = System.nanoTime();
		log.debug( "Avro serialization: " + ( ( end - begin ) / 1000000 ) );
		log.debug( "Avro message size: " + avroBytes.length );

		List<LuceneWork> copyOfWorks = null;
		begin = System.nanoTime();
		for ( int i = 0; i < loop; i++ ) {
			copyOfWorks = converter.toLuceneWorks( avroBytes );
		}
		end = System.nanoTime();
		log.debug( "Avro deserialization: " + ( ( end - begin ) / 1000000 ) );

		//make sure the compiler does not cheat
		log.debug( copyOfWorks == copyOfWorkForJavaSerial );

	}

	private List<LuceneWork> buildWorks() throws Exception {
		List<LuceneWork> works = new ArrayList<LuceneWork>();
		works.add( OptimizeLuceneWork.INSTANCE );
		works.add( OptimizeLuceneWork.INSTANCE );
		works.add( new OptimizeLuceneWork( RemoteEntity.class ) ); //class won't be send over
		works.add( new PurgeAllLuceneWork( RemoteEntity.class ) );
		works.add( new PurgeAllLuceneWork( RemoteEntity.class ) );
		works.add( new DeleteLuceneWork( 123l, "123", RemoteEntity.class ) );
		works.add( new DeleteLuceneWork( "Sissi", "Sissi", RemoteEntity.class ) );
		works.add(
				new DeleteLuceneWork(
						new URL( "http://emmanuelbernard.com" ),
						"http://emmanuelbernard.com",
						RemoteEntity.class
				)
		);

		Document doc = new Document();
		doc.setBoost( 2.3f );
		NumericField numField = new NumericField( "double", 23, Field.Store.NO, true );
		numField.setDoubleValue( 23d );
		numField.setOmitNorms( true );
		numField.setOmitTermFreqAndPositions( true );
		numField.setBoost( 3f );
		doc.add( numField );
		numField = new NumericField( "int", 23, Field.Store.NO, true );
		numField.setIntValue( 23 );
		doc.add( numField );
		numField = new NumericField( "float", 23, Field.Store.NO, true );
		numField.setFloatValue( 2.3f );
		doc.add( numField );
		numField = new NumericField( "long", 23, Field.Store.NO, true );
		numField.setLongValue( 23l );
		doc.add( numField );

		Map<String, String> analyzers = new HashMap<String, String>();
		analyzers.put( "godo", "ngram" );
		works.add( new AddLuceneWork( 123, "123", RemoteEntity.class, doc, analyzers ) );

		doc = new Document();
		doc.setBoost( 2.3f );
		Field field = new Field(
				"StringF",
				"String field",
				Field.Store.YES,
				Field.Index.ANALYZED,
				Field.TermVector.WITH_OFFSETS
		);
		field.setOmitNorms( true );
		field.setOmitTermFreqAndPositions( true );
		field.setBoost( 3f );
		doc.add( field );

		field = new Field(
				"StringF2",
				"String field 2",
				Field.Store.YES,
				Field.Index.ANALYZED,
				Field.TermVector.WITH_OFFSETS
		);
		doc.add( field );

		byte[] array = new byte[4];
		array[0] = 2;
		array[1] = 5;
		array[2] = 5;
		array[3] = 8;
		field = new Field( "binary", array, 0, array.length );
		doc.add( field );

		SerializableStringReader reader = new SerializableStringReader();
		field = new Field( "ReaderField", reader, Field.TermVector.WITH_OFFSETS );
		doc.add( field );

		List<List<AttributeImpl>> tokens = buildTokenSteamWithAttributes();

		CopyTokenStream tokenStream = new CopyTokenStream( tokens );
		field = new Field( "tokenstream", tokenStream, Field.TermVector.WITH_POSITIONS_OFFSETS );
		field.setOmitNorms( true );
		field.setOmitTermFreqAndPositions( true );
		field.setBoost( 3f );
		doc.add( field );

		works.add( new UpdateLuceneWork( 1234, "1234", RemoteEntity.class, doc ) );
		works.add( new AddLuceneWork( 125, "125", RemoteEntity.class, new Document() ) );
		return works;
	}

	private List<List<AttributeImpl>> buildTokenSteamWithAttributes() {
		List<List<AttributeImpl>> tokens = new ArrayList<List<AttributeImpl>>();
		tokens.add( new ArrayList<AttributeImpl>() );
		AnalysisRequestHandlerBase.TokenTrackingAttributeImpl attrImpl = new AnalysisRequestHandlerBase.TokenTrackingAttributeImpl();
		attrImpl.reset( new int[] { 1, 2, 3 }, 4 );
		tokens.get( 0 ).add( attrImpl );

		CharTermAttributeImpl charAttr = new CharTermAttributeImpl();
		charAttr.append( "Wazzza" );
		tokens.get( 0 ).add( charAttr );

		PayloadAttributeImpl payloadAttribute = new PayloadAttributeImpl();
		payloadAttribute.setPayload( new Payload( new byte[] { 0, 1, 2, 3 } ) );
		tokens.get( 0 ).add( payloadAttribute );

		KeywordAttributeImpl keywordAttr = new KeywordAttributeImpl();
		keywordAttr.setKeyword( true );
		tokens.get( 0 ).add( keywordAttr );

		PositionIncrementAttributeImpl posIncrAttr = new PositionIncrementAttributeImpl();
		posIncrAttr.setPositionIncrement( 3 );
		tokens.get( 0 ).add( posIncrAttr );

		FlagsAttributeImpl flagsAttr = new FlagsAttributeImpl();
		flagsAttr.setFlags( 435 );
		tokens.get( 0 ).add( flagsAttr );

		TypeAttributeImpl typeAttr = new TypeAttributeImpl();
		typeAttr.setType( "acronym" );
		tokens.get( 0 ).add( typeAttr );

		OffsetAttributeImpl offsetAttr = new OffsetAttributeImpl();
		offsetAttr.setOffset( 4, 7 );
		tokens.get( 0 ).add( offsetAttr );
		return tokens;
	}

	private void assertLuceneWork(LuceneWork work, LuceneWork copy) {
		assertThat( copy ).isInstanceOf( work.getClass() );
		if ( work instanceof OptimizeLuceneWork ) {
			assertNotNull( copy );
			assertTrue( copy instanceof OptimizeLuceneWork );
		}
		else if ( work instanceof PurgeAllLuceneWork ) {
			assertPurgeAll( (PurgeAllLuceneWork) work, (PurgeAllLuceneWork) copy );
		}
		else if ( work instanceof DeleteLuceneWork ) {
			assertDelete( (DeleteLuceneWork) work, (DeleteLuceneWork) copy );
		}
		else if ( work instanceof AddLuceneWork ) {
			assertAdd( (AddLuceneWork) work, (AddLuceneWork) copy );
		}
		else if ( work instanceof UpdateLuceneWork ) {
			assertUpdate( (UpdateLuceneWork) work, (UpdateLuceneWork) copy );
		}
		else {
			fail( "unexpected type" );
		}
	}

	private void assertAdd(AddLuceneWork work, AddLuceneWork copy) {
		assertThat( work.getEntityClass() ).as( "Add.getEntityClass is not copied" ).isEqualTo( copy.getEntityClass() );
		assertThat( work.getId() ).as( "Add.getId is not copied" ).isEqualTo( copy.getId() );
		assertThat( work.getIdInString() ).as( "Add.getIdInString is not the same" ).isEqualTo( copy.getIdInString() );
		assertThat( work.getFieldToAnalyzerMap() ).as( "Add.getFieldToAnalyzerMap is not the same" )
				.isEqualTo( copy.getFieldToAnalyzerMap() );
		assertDocument( work.getDocument(), copy.getDocument() );
	}

	private void assertUpdate(UpdateLuceneWork work, UpdateLuceneWork copy) {
		assertThat( work.getEntityClass() ).as( "Add.getEntityClass is not copied" ).isEqualTo( copy.getEntityClass() );
		assertThat( work.getId() ).as( "Add.getId is not copied" ).isEqualTo( copy.getId() );
		assertThat( work.getIdInString() ).as( "Add.getIdInString is not the same" ).isEqualTo( copy.getIdInString() );
		assertThat( work.getFieldToAnalyzerMap() ).as( "Add.getFieldToAnalyzerMap is not the same" )
				.isEqualTo( copy.getFieldToAnalyzerMap() );
		assertDocument( work.getDocument(), copy.getDocument() );
	}

	private void assertDocument(Document document, Document copy) {
		assertThat( document.getBoost() ).isEqualTo( copy.getBoost() );
		for ( int index = 0; index < document.getFields().size(); index++ ) {
			Fieldable field = document.getFields().get( index );
			Fieldable fieldCopy = copy.getFields().get( index );
			assertThat( field ).isInstanceOf( fieldCopy.getClass() );
			if ( field instanceof NumericField ) {
				assertNumericField( (NumericField) field, (NumericField) fieldCopy );
			}
			else if ( field instanceof Field ) {
				assertNormalField( (Field) field, (Field) fieldCopy );
			}
		}
	}

	private void assertNormalField(Field field, Field copy) {
		assertThat( copy.name() ).isEqualTo( field.name() );
		assertThat( copy.getBinaryLength() ).isEqualTo( field.getBinaryLength() );
		assertThat( copy.getBinaryOffset() ).isEqualTo( field.getBinaryOffset() );
		assertThat( copy.getBinaryValue() ).isEqualTo( field.getBinaryValue() );
		assertThat( copy.getBoost() ).isEqualTo( field.getBoost() );
		assertThat( copy.getOmitNorms() ).isEqualTo( field.getOmitNorms() );
		assertThat( copy.getOmitTermFreqAndPositions() ).isEqualTo( field.getOmitTermFreqAndPositions() );
		assertThat( copy.isBinary() ).isEqualTo( field.isBinary() );
		assertThat( copy.isIndexed() ).isEqualTo( field.isIndexed() );
		assertThat( copy.isLazy() ).isEqualTo( field.isLazy() );
		assertThat( copy.isStoreOffsetWithTermVector() ).isEqualTo( field.isStoreOffsetWithTermVector() );
		assertThat( copy.isStorePositionWithTermVector() ).isEqualTo( field.isStorePositionWithTermVector() );
		assertThat( copy.isStored() ).isEqualTo( field.isStored() );
		assertThat( copy.isTokenized() ).isEqualTo( field.isTokenized() );
		assertThat( compareReaders( copy.readerValue(), field.readerValue() ) ).isTrue();
		assertThat( compareTokenStreams( field.tokenStreamValue(), copy.tokenStreamValue() ) ).isTrue();
		assertThat( copy.stringValue() ).isEqualTo( field.stringValue() );

		assertThat( copy.isTermVectorStored() ).isEqualTo( field.isTermVectorStored() );
	}

	private boolean compareTokenStreams(TokenStream original, TokenStream copy) {
		if ( original == null ) {
			return copy == null;
		}
		try {
			original.reset();
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
		SerializableTokenStream serOriginal = CopyTokenStream.buildSerializabletokenStream( original );
		SerializableTokenStream serCopy = CopyTokenStream.buildSerializabletokenStream( copy );
		if ( serOriginal.getStream().size() != serCopy.getStream().size() ) {
			return false;
		}
		for ( int i = 0; i < serOriginal.getStream().size(); i++ ) {
			List<AttributeImpl> origToken = serOriginal.getStream().get( i );
			List<AttributeImpl> copyToken = serCopy.getStream().get( i );
			if ( origToken.size() != copyToken.size() ) {
				return false;
			}
			for ( int j = 0; j < origToken.size(); j++ ) {
				AttributeImpl origAttr = origToken.get( j );
				AttributeImpl copyAttr = copyToken.get( j );
				if ( origAttr.getClass() != copyAttr.getClass() ) {
					return false;
				}
				testAttributeTypes( origAttr, copyAttr );
			}
		}
		return true;
	}

	private void testAttributeTypes(AttributeImpl origAttr, AttributeImpl copyAttr) {
		if ( origAttr instanceof AnalysisRequestHandlerBase.TokenTrackingAttributeImpl ) {
			assertThat( ( (AnalysisRequestHandlerBase.TokenTrackingAttributeImpl) origAttr ).getPositions() )
					.isEqualTo( ( (AnalysisRequestHandlerBase.TokenTrackingAttributeImpl) copyAttr ).getPositions() );
		}
		else if ( origAttr instanceof CharTermAttribute ) {
			assertThat( origAttr.toString() ).isEqualTo( copyAttr.toString() );
		}
		else if ( origAttr instanceof PayloadAttribute ) {
			assertThat( ( (PayloadAttribute) origAttr ).getPayload() ).isEqualTo(
					( (PayloadAttribute) copyAttr ).getPayload()
			);
		}
		else if ( origAttr instanceof KeywordAttribute ) {
			assertThat( ( (KeywordAttribute) origAttr ).isKeyword() ).isEqualTo(
					( (KeywordAttribute) copyAttr ).isKeyword()
			);
		}
		else if ( origAttr instanceof PositionIncrementAttribute ) {
			assertThat( ( (PositionIncrementAttribute) origAttr ).getPositionIncrement() ).isEqualTo(
					( (PositionIncrementAttribute) copyAttr ).getPositionIncrement()
			);
		}
		else if ( origAttr instanceof FlagsAttribute ) {
			assertThat( ( (FlagsAttribute) origAttr ).getFlags() ).isEqualTo(
					( (FlagsAttribute) copyAttr ).getFlags()
			);
		}
		else if ( origAttr instanceof TypeAttribute ) {
			assertThat( ( (TypeAttribute) origAttr ).type() ).isEqualTo(
					( (TypeAttribute) copyAttr ).type()
			);
		}
		else if ( origAttr instanceof OffsetAttribute ) {
			OffsetAttribute orig = (OffsetAttribute) origAttr;
			OffsetAttribute cop = (OffsetAttribute) copyAttr;
			assertThat( orig.startOffset() ).isEqualTo( cop.startOffset() );
			assertThat( orig.endOffset() ).isEqualTo( cop.endOffset() );
		}
	}

	private boolean compareReaders(Reader copy, Reader original) {
		if ( original == null ) {
			return copy == null;
		}
		try {
			for ( int o = original.read(); o != -1; o = original.read() ) {
				int c = copy.read();
				if ( o != c ) {
					return false;
				}
			}
			return copy.read() == -1;
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private void assertNumericField(NumericField field, NumericField copy) {
		assertThat( copy.name() ).isEqualTo( field.name() );
		assertThat( copy.getBinaryLength() ).isEqualTo( field.getBinaryLength() );
		assertThat( copy.getBinaryOffset() ).isEqualTo( field.getBinaryOffset() );
		assertThat( copy.getBinaryValue() ).isEqualTo( field.getBinaryValue() );
		assertThat( copy.getBoost() ).isEqualTo( field.getBoost() );
		assertThat( copy.getDataType() ).isEqualTo( field.getDataType() );
		assertThat( copy.getNumericValue() ).isEqualTo( field.getNumericValue() );
		assertThat( copy.getOmitNorms() ).isEqualTo( field.getOmitNorms() );
		assertThat( copy.getOmitTermFreqAndPositions() ).isEqualTo( field.getOmitTermFreqAndPositions() );
		assertThat( copy.getPrecisionStep() ).isEqualTo( field.getPrecisionStep() );
		assertThat( copy.isBinary() ).isEqualTo( field.isBinary() );
		assertThat( copy.isIndexed() ).isEqualTo( field.isIndexed() );
		assertThat( copy.isLazy() ).isEqualTo( field.isLazy() );
		assertThat( copy.isStoreOffsetWithTermVector() ).isEqualTo( field.isStoreOffsetWithTermVector() );
		assertThat( copy.isStorePositionWithTermVector() ).isEqualTo( field.isStorePositionWithTermVector() );
		assertThat( copy.isStored() ).isEqualTo( field.isStored() );
		assertThat( copy.isTokenized() ).isEqualTo( field.isTokenized() );
		assertThat( copy.readerValue() ).isEqualTo( field.readerValue() );
		assertThat( copy.tokenStreamValue() ).isEqualTo( field.tokenStreamValue() );
		assertThat( copy.stringValue() ).isEqualTo( field.stringValue() );
	}

	private void assertDelete(DeleteLuceneWork work, DeleteLuceneWork copy) {
		assertThat( work.getEntityClass() ).as( "Delete.getEntityClass is not copied" )
				.isEqualTo( copy.getEntityClass() );
		assertThat( work.getId() ).as( "Delete.getId is not copied" ).isEqualTo( copy.getId() );
		assertThat( work.getDocument() ).as( "Delete.getDocument is not the same" ).isEqualTo( copy.getDocument() );
		assertThat( work.getIdInString() ).as( "Delete.getIdInString is not the same" )
				.isEqualTo( copy.getIdInString() );
		assertThat( work.getFieldToAnalyzerMap() ).as( "Delete.getFieldToAnalyzerMap is not the same" )
				.isEqualTo( copy.getFieldToAnalyzerMap() );
	}

	private void assertPurgeAll(PurgeAllLuceneWork work, PurgeAllLuceneWork copy) {
		assertThat( work.getEntityClass() ).as( "PurgeAllLuceneWork.getEntityClass is not copied" )
				.isEqualTo( copy.getEntityClass() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				RemoteEntity.class
		};
	}

	private static class SerializableStringReader extends Reader implements Serializable {
		private boolean read = false;

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			if ( read ) {
				return -1;
			}
			else {
				read = true;
				cbuf[off] = 2;
				return 1;
			}
		}

		@Override
		public void close() throws IOException {
		}

	}
}
