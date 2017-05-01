/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.indexes.serialization.impl.CopyTokenStream;
import org.hibernate.search.indexes.serialization.spi.SerializableTokenStream;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class SerializationTestHelper {

	private SerializationTestHelper() {
		//Utility class: not meant to be constructed
	}

	public static List<List<AttributeImpl>> buildTokenStreamWithAttributes() {
		List<List<AttributeImpl>> tokens = new ArrayList<>();
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

	public static void assertLuceneWorkList(List<LuceneWork> expectedWorkList, List<LuceneWork> actualWorkList) {
		assertThat( actualWorkList ).hasSize( expectedWorkList.size() );
		for ( int index = 0; index < expectedWorkList.size(); index++ ) {
			SerializationTestHelper.assertLuceneWork( expectedWorkList.get( index ), actualWorkList.get( index ) );
		}
	}

	public static void assertLuceneWork(LuceneWork work, LuceneWork copy) {
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
		else if ( work instanceof FlushLuceneWork ) {
			assertFlush( (FlushLuceneWork) work, (FlushLuceneWork) copy );
		}
		else if ( work instanceof DeleteByQueryLuceneWork ) {
			assertDeleteByQuery( (DeleteByQueryLuceneWork) work, (DeleteByQueryLuceneWork) copy );
		}
		else {
			fail( "unexpected type" );
		}
	}

	private static void assertAdd(AddLuceneWork work, AddLuceneWork copy) {
		assertThat( copy.getEntityType() ).as( "Add.getEntityClass is not copied" ).isEqualTo( work.getEntityType() );
		assertThat( copy.getId() ).as( "Add.getId is not copied" ).isEqualTo( work.getId() );
		assertThat( copy.getIdInString() ).as( "Add.getIdInString is not the same" ).isEqualTo( work.getIdInString() );
		assertThat( copy.getFieldToAnalyzerMap() ).as( "Add.getFieldToAnalyzerMap is not the same" )
				.isEqualTo( work.getFieldToAnalyzerMap() );
		assertDocument( work.getDocument(), copy.getDocument() );
	}

	private static void assertUpdate(UpdateLuceneWork work, UpdateLuceneWork copy) {
		assertThat( copy.getEntityType() ).as( "Add.getEntityClass is not copied" ).isEqualTo( work.getEntityType() );
		assertThat( copy.getId() ).as( "Add.getId is not copied" ).isEqualTo( work.getId() );
		assertThat( copy.getIdInString() ).as( "Add.getIdInString is not the same" ).isEqualTo( work.getIdInString() );
		assertThat( copy.getFieldToAnalyzerMap() ).as( "Add.getFieldToAnalyzerMap is not the same" )
				.isEqualTo( work.getFieldToAnalyzerMap() );
		assertDocument( work.getDocument(), copy.getDocument() );
	}

	private static void assertDeleteByQuery(DeleteByQueryLuceneWork work, DeleteByQueryLuceneWork copy) {
		assertThat( work.getEntityType() ).as( "DeleteByQuery.getEntityClass is not copied" ).isEqualTo( copy.getEntityType() );
		assertThat( work.getDeletionQuery() ).as( "DeleteByQuery.getDeletionQuery is not copied" ).isEqualTo( copy.getDeletionQuery() );
	}

	private static void assertDocument(Document original, Document copy) {
		assertEquals(
				"The serialized and de-serialized work list differ in size", original.getFields().size(),
				copy.getFields().size()
		);
		for ( int index = 0; index < original.getFields().size(); index++ ) {
			IndexableField field = original.getFields().get( index );
			IndexableField fieldCopy = copy.getFields().get( index );
			assertFieldEquality( (Field) field, (Field) fieldCopy );
		}
	}

	private static void assertFieldEquality(Field original, Field copy) {
		assertThat( copy.name() ).isEqualTo( original.name() );
		assertThat( copy.binaryValue() ).isEqualTo( original.binaryValue() );
		assertThat( copy.boost() ).isEqualTo( original.boost() );
		assertFieldType( copy.fieldType(), original.fieldType() );
		assertThat( compareReaders( copy.readerValue(), original.readerValue() ) ).isTrue();
		assertThat( compareTokenStreams( original.tokenStreamValue(), copy.tokenStreamValue() ) ).isTrue();
		assertThat( copy.stringValue() ).isEqualTo( original.stringValue() );
	}

	private static void assertFieldType(FieldType copy, FieldType original) {
		assertThat( copy.omitNorms() ).isEqualTo( original.omitNorms() );
		assertThat( copy.storeTermVectorOffsets() ).isEqualTo( original.storeTermVectorOffsets() );
		assertThat( copy.storeTermVectorPayloads() ).isEqualTo( original.storeTermVectorPayloads() );
		assertThat( copy.storeTermVectorOffsets() ).isEqualTo( original.storeTermVectorOffsets() );
		assertThat( copy.docValuesType() ).isEqualTo( original.docValuesType() );
		assertThat( copy.indexOptions() ).isEqualTo( original.indexOptions() );
		assertThat( copy.numericPrecisionStep() ).isEqualTo( original.numericPrecisionStep() );
		assertThat( copy.numericType() ).isEqualTo( original.numericType() );
		assertThat( copy.stored() ).isEqualTo( original.stored() );
		assertThat( copy.storeTermVectors() ).isEqualTo( original.storeTermVectors() );
		assertThat( copy.tokenized() ).isEqualTo( original.tokenized() );
		assertThat( copy.toString() ).isEqualTo( original.toString() );
	}

	private static boolean compareTokenStreams(TokenStream original, TokenStream copy) {
		if ( original == null ) {
			return copy == null;
		}
		SerializableTokenStream serOriginal = CopyTokenStream.buildSerializableTokenStream( original );
		SerializableTokenStream serCopy = CopyTokenStream.buildSerializableTokenStream( copy );
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

	private static void testAttributeTypes(AttributeImpl origAttr, AttributeImpl copyAttr) {
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

	private static boolean compareReaders(Reader copy, Reader original) {
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

	private static void assertDelete(DeleteLuceneWork work, DeleteLuceneWork copy) {
		assertThat( work.getEntityType() ).as( "Delete.getEntityClass is not copied" )
				.isEqualTo( copy.getEntityType() );
		assertThat( work.getId() ).as( "Delete.getId is not copied" ).isEqualTo( copy.getId() );
		assertThat( (Object) work.getDocument() ).as( "Delete.getDocument is not the same" )
				.isEqualTo( copy.getDocument() );
		assertThat( work.getIdInString() ).as( "Delete.getIdInString is not the same" )
				.isEqualTo( copy.getIdInString() );
		assertThat( work.getFieldToAnalyzerMap() ).as( "Delete.getFieldToAnalyzerMap is not the same" )
				.isEqualTo( copy.getFieldToAnalyzerMap() );
	}

	private static void assertPurgeAll(PurgeAllLuceneWork work, PurgeAllLuceneWork copy) {
		assertThat( work.getEntityType() ).as( "PurgeAllLuceneWork.getEntityClass is not copied" )
				.isEqualTo( copy.getEntityType() );
	}

	private static void assertFlush(FlushLuceneWork work, FlushLuceneWork copy) {
		assertThat( copy.getEntityType() ).as( "FlushLuceneWork.getEntityClass is not copied" )
				.isEqualTo( work.getEntityType() );
	}

	public static class SerializableStringReader extends Reader implements Serializable {
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
