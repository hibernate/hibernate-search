/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.serialization.avro.impl.AvroSerializationProvider;
import org.hibernate.search.indexes.serialization.impl.CopyTokenStream;
import org.hibernate.search.indexes.serialization.impl.LuceneWorkSerializerImpl;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializableTokenStream;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SerializationTest {

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder( RemoteEntity.class );

	private SerializationProvider serializationProvider;

	@Before
	public void setUp() {
		ServiceManager serviceManager = new StandardServiceManager(
				new SearchConfigurationForTest(),
				null
		);

		serializationProvider = serviceManager.requestService( SerializationProvider.class );
		assertTrue( "Wrong serialization provider", serializationProvider instanceof AvroSerializationProvider );
	}

	@Test
	public void testAvroSerialization() throws Exception {


		LuceneWorkSerializer converter = new LuceneWorkSerializerImpl(
				serializationProvider,
				searchFactoryHolder.getSearchFactory()
		);
		List<LuceneWork> works = buildWorks();

		byte[] bytes = converter.toSerializedModel( works );
		List<LuceneWork> copyOfWorks = converter.toLuceneWorks( bytes );

		assertThat( copyOfWorks ).hasSize( works.size() );
		for ( int index = 0; index < works.size(); index++ ) {
			assertLuceneWork( works.get( index ), copyOfWorks.get( index ) );
		}
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
		Field numField = new DoubleField( "double", 23d, Store.NO );
		//numField.setBoost( 3f );
		//numField.setOmitNorms( true );
		//numField.setOmitTermFreqAndPositions( true );
		doc.add( numField );
		numField = new IntField( "int", 23, Store.NO );
		doc.add( numField );
		numField = new FloatField( "float", 2.3f, Store.NO );
		doc.add( numField );
		numField = new LongField( "long", 23l, Store.NO );
		doc.add( numField );

		Map<String, String> analyzers = new HashMap<String, String>();
		analyzers.put( "godo", "ngram" );
		works.add( new AddLuceneWork( 123, "123", RemoteEntity.class, doc, analyzers ) );

		doc = new Document();
		Field field = new Field(
				"StringF",
				"String field",
				Field.Store.YES,
				Field.Index.ANALYZED,
				Field.TermVector.WITH_OFFSETS
		);
//		field.setOmitNorms( true );
//		field.setOmitTermFreqAndPositions( true );
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
//		field.setOmitNorms( true );
//		field.setOmitTermFreqAndPositions( true );
		field.setBoost( 3f );
		doc.add( field );

		works.add( new UpdateLuceneWork( 1234, "1234", RemoteEntity.class, doc ) );
		works.add( new AddLuceneWork( 125, "125", RemoteEntity.class, new Document() ) );
		return works;
	}

	private List<List<AttributeImpl>> buildTokenSteamWithAttributes() {
		List<List<AttributeImpl>> tokens = new ArrayList<List<AttributeImpl>>();
		tokens.add( new ArrayList<AttributeImpl>() );

		CharTermAttributeImpl charAttr = new CharTermAttributeImpl();
		charAttr.append( "Wazzza" );
		tokens.get( 0 ).add( charAttr );

		PayloadAttributeImpl payloadAttribute = new PayloadAttributeImpl();
		payloadAttribute.setPayload( new BytesRef( new byte[] { 0, 1, 2, 3 } ) );
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

	private void assertDocument(Document original, Document copy) {
		assertThat( original.getFields().size() ).isEqualTo( copy.getFields().size() );
		for ( int index = 0; index < original.getFields().size(); index++ ) {
			IndexableField field = original.getFields().get( index );
			IndexableField fieldCopy = copy.getFields().get( index );
			assertFieldEquality( (Field) field, (Field) fieldCopy );
		}
	}

	private void assertFieldEquality(Field original, Field copy) {
		assertThat( copy.name() ).isEqualTo( original.name() );
		assertThat( copy.binaryValue() ).isEqualTo( original.binaryValue() );
		assertThat( copy.boost() ).isEqualTo( original.boost() );
		assertFieldType( copy.fieldType(), original.fieldType() );
		assertThat( compareReaders( copy.readerValue(), original.readerValue() ) ).isTrue();
		assertThat( compareTokenStreams( original.tokenStreamValue(), copy.tokenStreamValue() ) ).isTrue();
		assertThat( copy.stringValue() ).isEqualTo( original.stringValue() );
	}

	private void assertFieldType(FieldType copy, FieldType original) {
		assertThat( original.omitNorms() ).isEqualTo( copy.omitNorms() );
		assertThat( original.storeTermVectorOffsets() ).isEqualTo( copy.storeTermVectorOffsets() );
		assertThat( original.storeTermVectorPayloads() ).isEqualTo( copy.storeTermVectorPayloads() );
		assertThat( original.storeTermVectorOffsets() ).isEqualTo( copy.storeTermVectorOffsets() );
		assertThat( original.docValueType() ).isEqualTo( copy.docValueType() );
		assertThat( original.indexed() ).isEqualTo( copy.indexed() );
		assertThat( original.indexOptions() ).isEqualTo( copy.indexOptions() );
		assertThat( original.numericPrecisionStep() ).isEqualTo( copy.numericPrecisionStep() );
		assertThat( original.numericType() ).isEqualTo( copy.numericType() );
		assertThat( original.stored() ).isEqualTo( copy.stored() );
		assertThat( original.storeTermVectors() ).isEqualTo( copy.storeTermVectors() );
		assertThat( original.tokenized() ).isEqualTo( copy.tokenized() );
		assertThat( original.toString() ).isEqualTo( copy.toString() );
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
		if ( origAttr instanceof CharTermAttribute ) {
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
		else {
			Assert.fail( "Unexpected Attribute implementation received" );
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

	private void assertDelete(DeleteLuceneWork work, DeleteLuceneWork copy) {
		assertThat( work.getEntityClass() ).as( "Delete.getEntityClass is not copied" )
				.isEqualTo( copy.getEntityClass() );
		assertThat( work.getId() ).as( "Delete.getId is not copied" ).isEqualTo( copy.getId() );
		assertThat( (Object) work.getDocument() ).as( "Delete.getDocument is not the same" )
				.isEqualTo( copy.getDocument() );
		assertThat( work.getIdInString() ).as( "Delete.getIdInString is not the same" )
				.isEqualTo( copy.getIdInString() );
		assertThat( work.getFieldToAnalyzerMap() ).as( "Delete.getFieldToAnalyzerMap is not the same" )
				.isEqualTo( copy.getFieldToAnalyzerMap() );
	}

	private void assertPurgeAll(PurgeAllLuceneWork work, PurgeAllLuceneWork copy) {
		assertThat( work.getEntityClass() ).as( "PurgeAllLuceneWork.getEntityClass is not copied" )
				.isEqualTo( copy.getEntityClass() );
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
