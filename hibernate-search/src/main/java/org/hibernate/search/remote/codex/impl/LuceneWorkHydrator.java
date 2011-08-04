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
package org.hibernate.search.remote.codex.impl;

import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.util.AttributeImpl;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.remote.codex.spi.LuceneHydrator;
import org.hibernate.search.remote.operations.impl.Index;
import org.hibernate.search.remote.operations.impl.Store;
import org.hibernate.search.remote.operations.impl.TermVector;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LuceneWorkHydrator implements LuceneHydrator {
	private SearchFactoryImplementor searchFactory;
	private List<LuceneWork> results;
	private ClassLoader loader;
	private Document luceneDocument;

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
		results.add( new OptimizeLuceneWork() );
	}

	@Override
	public void addPurgeAllLuceneWork(String entityClassName) {
		Class<?> entityClass = ClassLoaderHelper.classForName( entityClassName, LuceneWorkHydrator.class, "entity class" );
		results.add( new PurgeAllLuceneWork( entityClass ) );
	}

	@Override
	public void addDeleteLuceneWork(String entityClassName, Serializable id) {
		Class<?> entityClass = ClassLoaderHelper.classForName( entityClassName, LuceneWorkHydrator.class, "entity class" );
		LuceneWork result = new DeleteLuceneWork(
				id,
				objectIdInString(entityClass, id),
				entityClass
		);
		results.add( result );
	}

	@Override
	public void addAddLuceneWork(String entityClassName, Serializable id, Map<String, String> fieldToAnalyzerMap) {
		Class<?> entityClass = ClassLoaderHelper.classForName( entityClassName, LuceneWorkHydrator.class, "entity class" );
		LuceneWork result = new AddLuceneWork(
				id,
				objectIdInString(entityClass, id),
				entityClass,
				getLuceneDocument(),
				fieldToAnalyzerMap
		);
		results.add( result );
		clearDocument();
	}

	@Override
	public void addUpdateLuceneWork(String entityClassName, Serializable id, Map<String, String> fieldToAnalyzerMap) {
		Class<?> entityClass = ClassLoaderHelper.classForName( entityClassName, LuceneWorkHydrator.class, "entity class" );
		LuceneWork result = new AddLuceneWork(
				id,
				objectIdInString(entityClass, id),
				entityClass,
				getLuceneDocument(),
				fieldToAnalyzerMap
		);
		results.add( result );
		clearDocument();
	}

	private void clearDocument() {
		luceneDocument = null;
	}

	@Override
	public void defineDocument(float boost) {
		getLuceneDocument().setBoost( boost );
	}

	@Override
	public void addFieldable(Serializable instance) {
		getLuceneDocument().add( ( Fieldable ) instance );
	}

	@Override
	public void addIntNumericField(int value, String name, int precisionStep, Store store, boolean indexed, boolean omitNorms, boolean omitTermFreqAndPositions) {
		NumericField numField = new NumericField(
						name,
						precisionStep,
						getStore( store ),
						indexed);
		numField.setOmitNorms( omitNorms );
		numField.setOmitTermFreqAndPositions( omitTermFreqAndPositions );
		numField.setIntValue( value );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addLongNumericField(long value, String name, int precisionStep, Store store, boolean indexed, boolean omitNorms, boolean omitTermFreqAndPositions) {
		NumericField numField = new NumericField(
						name,
						precisionStep,
						getStore( store ),
						indexed);
		numField.setOmitNorms( omitNorms );
		numField.setOmitTermFreqAndPositions( omitTermFreqAndPositions );
		numField.setLongValue( value );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addFloatNumericField(float value, String name, int precisionStep, Store store, boolean indexed, boolean omitNorms, boolean omitTermFreqAndPositions) {
		NumericField numField = new NumericField(
						name,
						precisionStep,
						getStore( store ),
						indexed);
		numField.setOmitNorms( omitNorms );
		numField.setOmitTermFreqAndPositions( omitTermFreqAndPositions );
		numField.setFloatValue( value );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addDoubleNumericField(double value, String name, int precisionStep, Store store, boolean indexed, boolean omitNorms, boolean omitTermFreqAndPositions) {
		NumericField numField = new NumericField(
						name,
						precisionStep,
						getStore( store ),
						indexed);
		numField.setOmitNorms( omitNorms );
		numField.setOmitTermFreqAndPositions( omitTermFreqAndPositions );
		numField.setDoubleValue( value );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addFieldWithBinaryData(String name, byte[] value, int offset, int length, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		Field luceneField = new Field(name, value, offset, length);
		setCommonFieldAttributesAddAddToDocument( boost, omitNorms, omitTermFreqAndPositions, luceneField );
	}

	private void setCommonFieldAttributesAddAddToDocument(float boost, boolean omitNorms, boolean omitTermFreqAndPositions, Field luceneField) {
		luceneField.setBoost( boost );
		luceneField.setOmitNorms( omitNorms );
		luceneField.setOmitTermFreqAndPositions( omitTermFreqAndPositions );
		getLuceneDocument().add( luceneField );
	}

	@Override
	public void addFieldWithStringData(String name, String value, Store store, Index index, TermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		Field luceneField = new Field( name, value, getStore( store ), getIndex( index ), getTermVector( termVector ) );
		setCommonFieldAttributesAddAddToDocument( boost, omitNorms, omitTermFreqAndPositions, luceneField );
	}

	@Override
	public void addFieldWithTokenStreamData(String name, List<List<AttributeImpl>> tokenStream, TermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		Field luceneField = new Field( name, new CopyTokenStream(tokenStream), getTermVector( termVector ) );
		setCommonFieldAttributesAddAddToDocument( boost, omitNorms, omitTermFreqAndPositions, luceneField );
	}

	@Override
	public void addFieldWithSerializableReaderData(String name, Reader value, TermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		Field luceneField = new Field( name, value, getTermVector( termVector ) );
		setCommonFieldAttributesAddAddToDocument( boost, omitNorms, omitTermFreqAndPositions, luceneField );
	}

	private Document getLuceneDocument() {
		if (luceneDocument == null) {
			luceneDocument = new Document();
		}
		return luceneDocument;
	}

	private String objectIdInString(Class<?> entityClass, Serializable id) {
		EntityIndexBinder<?> indexBindingForEntity = searchFactory.getIndexBindingForEntity( entityClass );
		if (indexBindingForEntity == null) {
			throw new SearchException( "Unable to find entity type metadata while deserializing: " + entityClass );
		}
		DocumentBuilderIndexedEntity<?> documentBuilder = indexBindingForEntity.getDocumentBuilder();
		return documentBuilder.objectToString( documentBuilder.getIdKeywordName(), id );
	}

	private static Field.TermVector getTermVector(TermVector termVector) {
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
				throw new SearchException( "Unable to convert serializable TermVector to Lucene TermVector: " + termVector );
		}
	}

	private static Field.Index getIndex(Index index) {
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
				throw new SearchException( "Unable to convert serializable Index to Lucene Index: " + index );
		}
	}

	private static Field.Store getStore(Store store) {
		switch ( store ) {
			case NO:
				return Field.Store.NO;
			case YES:
				return Field.Store.YES;
			default:
				throw new SearchException( "Unable to convert serializable Store to Lucene Store: " + store );
		}
	}
}
