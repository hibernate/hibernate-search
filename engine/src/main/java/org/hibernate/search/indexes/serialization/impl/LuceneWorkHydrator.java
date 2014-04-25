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
package org.hibernate.search.indexes.serialization.impl;

import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorksBuilder;
import org.hibernate.search.indexes.serialization.spi.SerializableIndex;
import org.hibernate.search.indexes.serialization.spi.SerializableStore;
import org.hibernate.search.indexes.serialization.spi.SerializableTermVector;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.indexes.serialization.impl.SerializationHelper.toSerializable;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LuceneWorkHydrator implements LuceneWorksBuilder {
	private static final Log log = LoggerFactory.make();

	private SearchFactoryImplementor searchFactory;
	private List<LuceneWork> results;
	private ClassLoader loader;
	private Document luceneDocument;
	private List<AttributeImpl> attributes;
	private List<List<AttributeImpl>> tokens;
	private Serializable id;

	public LuceneWorkHydrator(SearchFactoryImplementor searchFactory) {
		this.searchFactory = searchFactory;
		this.results = new ArrayList<LuceneWork>();
		this.loader = Thread.currentThread().getContextClassLoader();
	}

	public List<LuceneWork> getLuceneWorks() {
		return results;
	}

	@Override
	public void addOptimizeAll() {
		results.add( OptimizeLuceneWork.INSTANCE );
	}

	@Override
	public void addPurgeAllLuceneWork(String entityClassName) {
		Class<?> entityClass = ClassLoaderHelper.classForName(
				entityClassName,
				"entity class",
				searchFactory.getServiceManager()
		);
		results.add( new PurgeAllLuceneWork( entityClass ) );
	}

	@Override
	public void addIdAsJavaSerialized(byte[] idAsByte) {
		this.id = toSerializable( idAsByte, loader );
	}

	@Override
	public void addId(Serializable id) {
		this.id = id;
	}

	@Override
	public void addDeleteLuceneWork(String entityClassName, ConversionContext conversionContext) {
		Class<?> entityClass = ClassLoaderHelper.classForName(
				entityClassName,
				"entity class",
				searchFactory.getServiceManager()
		);
		LuceneWork result = new DeleteLuceneWork(
				id,
				objectIdInString( entityClass, id, conversionContext ),
				entityClass
		);
		results.add( result );
		id = null;
	}

	@Override
	public void addAddLuceneWork(String entityClassName, Map<String, String> fieldToAnalyzerMap, ConversionContext conversionContext) {
		Class<?> entityClass = ClassLoaderHelper.classForName(
				entityClassName,
				"entity class",
				searchFactory.getServiceManager()
		);
		LuceneWork result = new AddLuceneWork(
				id,
				objectIdInString( entityClass, id, conversionContext ),
				entityClass,
				getLuceneDocument(),
				fieldToAnalyzerMap
		);
		results.add( result );
		clearDocument();
		id = null;
	}

	@Override
	public void addUpdateLuceneWork(String entityClassName, Map<String, String> fieldToAnalyzerMap, ConversionContext conversionContext) {
		Class<?> entityClass = ClassLoaderHelper.classForName(
				entityClassName,
				"entity class",
				searchFactory.getServiceManager()
		);
		LuceneWork result = new UpdateLuceneWork(
				id,
				objectIdInString( entityClass, id, conversionContext ),
				entityClass,
				getLuceneDocument(),
				fieldToAnalyzerMap
		);
		results.add( result );
		clearDocument();
		id = null;
	}

	private void clearDocument() {
		luceneDocument = null;
	}

	@Override
	public void defineDocument() {
		//Document level boost is not available anymore: is this method still needed?
		getLuceneDocument();
	}

	@Override
	public void addFieldable(byte[] instanceAsByte) {
		//FIXME implementors of IndexableField ARE NOT SERIALIZABLE :-(
		getLuceneDocument().add( (IndexableField) toSerializable( instanceAsByte, loader ) );
	}

	@Override
	public void addIntNumericField(int value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final IntField numField = new IntField( name, value, getStore( store ) );
		numField.setBoost( boost );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addLongNumericField(long value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final LongField numField = new LongField( name, value, getStore( store ) );
		numField.setBoost( boost );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addFloatNumericField(float value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final FloatField numField = new FloatField( name, value, getStore( store ) );
		numField.setBoost( boost );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addDoubleNumericField(double value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final DoubleField numField = new DoubleField( name, value, getStore( store ) );
		numField.setBoost( boost );
		getLuceneDocument().add( numField );
	}

	/*
	 * TODO FieldType has several more attributes currently ignored. This is temporarily meant to not break too much Lucene 3x code.
	 * Also: if we move to a static index schema serializing the field attributes should be unnecessary.
	 */
	private FieldType buildFieldType(int precisionStep, SerializableStore store, boolean indexed, boolean omitNorms,
			boolean omitTermFreqAndPositions, NumericType numericFieldType) {
		final FieldType type = new FieldType();
		type.setNumericPrecisionStep( precisionStep );
		type.setStored( store == SerializableStore.YES );
		type.setIndexOptions( omitTermFreqAndPositions ? IndexOptions.DOCS_AND_FREQS : IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
		type.setOmitNorms( omitNorms );
		type.setIndexed( indexed );
		type.setNumericType( numericFieldType );
		return type;
	}

	@Deprecated //removed soon
	public void addFieldWithBinaryData(String name, byte[] value, int offset, int length, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		addFieldWithBinaryData( name, value, offset, length );
	}

	@Override
	public void addFieldWithBinaryData(String name, byte[] value, int offset, int length) {
		Field luceneField = new StoredField( name, value, offset, length );
		getLuceneDocument().add( luceneField );
	}

	@Override
	public void addFieldWithStringData(String name, String value, SerializableStore store, SerializableIndex index, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		FieldType type = Field.translateFieldType( getStore( store ), getIndex( index ), getTermVector( termVector ) );
		//type.setOmitNorms( omitNorms );
		//type.setStoreTermVectorPositions( ! omitTermFreqAndPositions );
		Field luceneField = new Field( name, value, type );
		luceneField.setBoost( boost );
		getLuceneDocument().add( luceneField );
	}

	@Override
	public void addFieldWithTokenStreamData(String name, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		FieldType type = Field.translateFieldType( Store.NO, Index.ANALYZED, getTermVector( termVector ) );
		//type.setOmitNorms( omitNorms );
		//type.setStoreTermVectorPositions( ! omitTermFreqAndPositions );
		Field luceneField = new Field( name, new CopyTokenStream( tokens ), type );
		luceneField.setBoost( boost );
		getLuceneDocument().add( luceneField );
		clearTokens();
	}

	private void clearTokens() {
		tokens = new ArrayList<List<AttributeImpl>>();
	}

	@Override
	public void addFieldWithSerializableReaderData(String name, byte[] valueAsByte, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		FieldType type = Field.translateFieldType( Store.NO, Index.ANALYZED, getTermVector( termVector ) );
		//type.setOmitNorms( omitNorms );
		//type.setStoreTermVectorPositions( ! omitTermFreqAndPositions );
		Reader value = (Reader) toSerializable( valueAsByte, loader );
		Field luceneField = new Field( name, value, type );
		luceneField.setBoost( boost );
		getLuceneDocument().add( luceneField );
	}

	@Override
	public void addSerializedAttribute(byte[] bytes) {
		getAttributes().add( (AttributeImpl) toSerializable( bytes, loader ) );
	}

	@Override
	public void addAttributeInstance(AttributeImpl attribute) {
		getAttributes().add( attribute );
	}

	@Override
	public void addTokenTrackingAttribute(List<Integer> positions) {
		//TokenTrackingAttribute is no longer available
		throw new SearchException( "Serialization of TokenTrackingAttribute is no longer supported" );
	}

	@Override
	public void addCharTermAttribute(CharSequence sequence) {
		AttributeImpl attr = AttributeSource.AttributeFactory
				.DEFAULT_ATTRIBUTE_FACTORY
				.createAttributeInstance( CharTermAttribute.class );
		( (CharTermAttribute) attr ).append( sequence );
		getAttributes().add( attr );
	}

	@Override
	public void addPayloadAttribute(byte[] payloads) {
		AttributeImpl attr = AttributeSource.AttributeFactory
				.DEFAULT_ATTRIBUTE_FACTORY
				.createAttributeInstance( PayloadAttribute.class );
		( (PayloadAttribute) attr ).setPayload( new BytesRef( payloads ) );
		getAttributes().add( attr );
	}

	@Override
	public void addKeywordAttribute(boolean isKeyword) {
		AttributeImpl attr = AttributeSource.AttributeFactory
				.DEFAULT_ATTRIBUTE_FACTORY
				.createAttributeInstance( KeywordAttribute.class );
		( (KeywordAttribute) attr ).setKeyword( isKeyword );
		getAttributes().add( attr );
	}

	@Override
	public void addPositionIncrementAttribute(int positionIncrement) {
		AttributeImpl attr = AttributeSource.AttributeFactory
				.DEFAULT_ATTRIBUTE_FACTORY
				.createAttributeInstance( PositionIncrementAttribute.class );
		( (PositionIncrementAttribute) attr ).setPositionIncrement( positionIncrement );
		getAttributes().add( attr );
	}

	@Override
	public void addFlagsAttribute(int flags) {
		AttributeImpl attr = AttributeSource.AttributeFactory
				.DEFAULT_ATTRIBUTE_FACTORY
				.createAttributeInstance( FlagsAttribute.class );
		( (FlagsAttribute) attr ).setFlags( flags );
		getAttributes().add( attr );
	}

	@Override
	public void addTypeAttribute(String type) {
		AttributeImpl attr = AttributeSource.AttributeFactory
				.DEFAULT_ATTRIBUTE_FACTORY
				.createAttributeInstance( TypeAttribute.class );
		( (TypeAttribute) attr ).setType( type );
		getAttributes().add( attr );
	}

	@Override
	public void addOffsetAttribute(int startOffset, int endOffset) {
		AttributeImpl attr = AttributeSource.AttributeFactory
				.DEFAULT_ATTRIBUTE_FACTORY
				.createAttributeInstance( OffsetAttribute.class );
		( (OffsetAttribute) attr ).setOffset( startOffset, endOffset );
		getAttributes().add( attr );
	}

	@Override
	public void addToken() {
		getTokens().add( getAttributes() );
		clearAttributes();
	}

	private void clearAttributes() {
		attributes = new ArrayList<AttributeImpl>();
	}

	private Document getLuceneDocument() {
		if ( luceneDocument == null ) {
			luceneDocument = new Document();
		}
		return luceneDocument;
	}

	private String objectIdInString(Class<?> entityClass, Serializable id, ConversionContext conversionContext) {
		EntityIndexBinding indexBindingForEntity = searchFactory.getIndexBinding( entityClass );
		if ( indexBindingForEntity == null ) {
			throw new SearchException( "Unable to find entity type metadata while deserializing: " + entityClass );
		}
		DocumentBuilderIndexedEntity<?> documentBuilder = indexBindingForEntity.getDocumentBuilder();
		return documentBuilder.objectToString( documentBuilder.getIdKeywordName(), id, conversionContext );
	}

	private static Field.TermVector getTermVector(SerializableTermVector termVector) {
		switch ( termVector ) {
			case NO:
				return Field.TermVector.NO;
			case WITH_OFFSETS:
				return Field.TermVector.WITH_OFFSETS;
			case WITH_POSITIONS:
				return Field.TermVector.WITH_POSITIONS;
			case WITH_POSITIONS_OFFSETS:
				return Field.TermVector.WITH_POSITIONS_OFFSETS;
			case YES:
				return Field.TermVector.YES;
			default:
				throw log.unableToConvertSerializableTermVectorToLuceneTermVector( termVector.toString() );
		}
	}

	private static Field.Index getIndex(SerializableIndex index) {
		switch ( index ) {
			case ANALYZED:
				return Field.Index.ANALYZED;
			case ANALYZED_NO_NORMS:
				return Field.Index.ANALYZED_NO_NORMS;
			case NO:
				return Field.Index.NO;
			case NOT_ANALYZED:
				return Field.Index.NOT_ANALYZED;
			case NOT_ANALYZED_NO_NORMS:
				return Field.Index.NOT_ANALYZED_NO_NORMS;
			default:
				throw log.unableToConvertSerializableIndexToLuceneIndex( index.toString() );
		}
	}

	private static Field.Store getStore(SerializableStore store) {
		switch ( store ) {
			case NO:
				return Field.Store.NO;
			case YES:
				return Field.Store.YES;
			default:
				throw log.unableToConvertSerializableStoreToLuceneStore( store.toString() );
		}
	}

	public List<AttributeImpl> getAttributes() {
		if ( attributes == null ) {
			attributes = new ArrayList<AttributeImpl>();
		}
		return attributes;
	}

	public List<List<AttributeImpl>> getTokens() {
		if ( tokens == null ) {
			tokens = new ArrayList<List<AttributeImpl>>();
		}
		return tokens;
	}
}
