/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine.impl;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;

/**
 * A wrapper class for Lucene parameters needed for indexing.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 * @author Gustavo Fernandes
 */
public class LuceneOptionsImpl implements LuceneOptions {

	private static final FieldType TYPE_COMPRESSED = new FieldType();
	static {
		TYPE_COMPRESSED.setIndexed( false );
		TYPE_COMPRESSED.setTokenized( false );
		TYPE_COMPRESSED.setOmitNorms( true );
		TYPE_COMPRESSED.setIndexOptions( IndexOptions.DOCS_ONLY );
		TYPE_COMPRESSED.setStored( true );
		TYPE_COMPRESSED.freeze();
	}

	private final boolean storeCompressed;
	private final boolean storeUncompressed;
	private boolean documentBoostApplied = false; //needs to be applied only once
	private final float fieldLevelBoost;
	private final float documentLevelBoost;
	private final Index indexMode;
	private final TermVector termVector;
	private final Store storeType;
	private final int precisionStep;
	private final String indexNullAs;

	@Deprecated
	public LuceneOptionsImpl(DocumentFieldMetadata fieldMetadata) {
		this( fieldMetadata, 1f, 1f );
	}

	public LuceneOptionsImpl(DocumentFieldMetadata fieldMetadata, float fieldLevelBoost, float documentLevelBoost) {
		this.documentLevelBoost = documentLevelBoost;
		this.indexMode = fieldMetadata.getIndex();
		this.termVector = fieldMetadata.getTermVector();
		this.fieldLevelBoost = fieldLevelBoost;
		this.storeType = fieldMetadata.getStore();
		this.storeCompressed = this.storeType.equals( Store.COMPRESS );
		this.storeUncompressed = this.storeType.equals( Store.YES );
		this.indexNullAs = fieldMetadata.indexNullAs();
		this.precisionStep = fieldMetadata.getPrecisionStep();
	}

	@Override
	public void addFieldToDocument(String name, String indexedString, Document document) {
		//Do not add fields on empty strings, seems a sensible default in most situations
		if ( StringHelper.isNotEmpty( indexedString ) ) {
			if ( !( indexMode.equals( Index.NO ) && storeCompressed ) ) {
				standardFieldAdd( name, indexedString, document );
			}
			if ( storeCompressed ) {
				compressedFieldAdd( name, indexedString, document );
			}
		}
	}

	private void standardFieldAdd(String name, String indexedString, Document document) {
		Field field = new Field( name, indexedString, storeUncompressed ? Field.Store.YES : Field.Store.NO, indexMode, termVector );
		setBoost( field );
		document.add( field );
	}

	private void compressedFieldAdd(String name, String indexedString, Document document) {
		byte[] compressedString = CompressionTools.compressString( indexedString );
		// indexed is implicitly set to false when using byte[]
		Field field = new Field( name, compressedString, TYPE_COMPRESSED );
		document.add( field );
	}

	@Override
	public float getBoost() {
		return fieldLevelBoost;
	}

	@Override
	public String indexNullAs() {
		return indexNullAs;
	}

	@Override
	public boolean isCompressed() {
		return storeCompressed;
	}

	@Override
	public Index getIndex() {
		return this.indexMode;
	}

	@Override
	public org.apache.lucene.document.Field.Store getStore() {
		if ( storeUncompressed || storeCompressed ) {
			return org.apache.lucene.document.Field.Store.YES;
		}
		else {
			return org.apache.lucene.document.Field.Store.NO;
		}
	}

	@Override
	public TermVector getTermVector() {
		return this.termVector;
	}

	private void setBoost(Field field) {
		/**
		 * MEMO from the Apache Lucene documentation:
		 * It is illegal to return a boost other than 1.0f for a field that is not
		 * indexed ({@link IndexableFieldType#indexed()} is false) or omits normalization values
		 * ({@link IndexableFieldType#omitNorms()} returns true).
		 */
		if ( indexMode.isIndexed() && ! indexMode.omitNorms() ) {
			if ( documentBoostApplied == false ) {
				documentBoostApplied = true;
				//FIXME This isn't entirely accurate as in some cases the LuceneOptionsImpl
				//is being reused for multiple fields: this needs to be significantly different,
				//potentially dropping the LuceneOptionsImpl usage.
				field.setBoost( fieldLevelBoost * documentLevelBoost );
			}
			else {
				field.setBoost( fieldLevelBoost );
			}
		}
	}

	private void checkNotCompressed(final String fieldName) {
		//TODO this sanity check should be done at bootstrap, not runtime
		if ( storeType == Store.COMPRESS ) {
			throw new SearchException( "Error indexing field " + fieldName + ", @NumericField cannot be compressed" );
		}
	}

	@Override
	public void addDoubleFieldToDocument(String fieldName, double doubleValue, Document document) {
		checkNotCompressed( fieldName );
		DoubleField field = new DoubleField( fieldName, doubleValue, storeType != Store.NO ? Field.Store.YES : Field.Store.NO );
		setBoost( field );
		document.add( field );
	}

	@Override
	public void addFloatFieldToDocument(String fieldName, float floatValue, Document document) {
		checkNotCompressed( fieldName );
		FloatField field = new FloatField( fieldName, floatValue, storeType != Store.NO ? Field.Store.YES : Field.Store.NO );
		setBoost( field );
		document.add( field );
	}

	@Override
	public void addIntFieldToDocument(String fieldName, int intValue, Document document) {
		checkNotCompressed( fieldName );
		IntField field = new IntField( fieldName, intValue, storeType != Store.NO ? Field.Store.YES : Field.Store.NO );
		setBoost( field );
		document.add( field );
	}

	@Override
	public void addLongFieldToDocument(String fieldName, long longValue, Document document) {
		checkNotCompressed( fieldName );
		LongField field = new LongField( fieldName, longValue, storeType != Store.NO ? Field.Store.YES : Field.Store.NO );
		setBoost( field );
		document.add( field );
	}

	/**
	 * @deprecated See {@link LuceneOptions#addNumericFieldToDocument(String, Number, Document)}
	 */
	@Override @Deprecated
	public void addNumericFieldToDocument(String fieldName, Number indexedValue, Document document) {
		if ( indexedValue == null ) {
			throw new IllegalArgumentException( "the indexedValue parameter shall not be null" );
		}
		if ( indexedValue instanceof Double ) {
			addDoubleFieldToDocument( fieldName, ( (Double) indexedValue ).doubleValue(), document );
		}
		else if ( indexedValue instanceof Float ) {
			addDoubleFieldToDocument( fieldName, ( (Float) indexedValue ).floatValue(), document );
		}
		else if ( indexedValue instanceof Integer ) {
			addIntFieldToDocument( fieldName, ( (Integer) indexedValue ).intValue(), document );
		}
		else if ( indexedValue instanceof Float ) {
			addLongFieldToDocument( fieldName, ( (Long) indexedValue ).longValue(), document );
		}
		else {
			throw new IllegalArgumentException( "unsupported type of Number" );
		}
	}

}
