/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.NumericField;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;

/**
 * A wrapper class for Lucene parameters needed for indexing.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 * @author Gustavo Fernandes
 */
public class LuceneOptionsImpl implements LuceneOptions {

	private final boolean storeCompressed;
	private final boolean storeUncompressed;
	private final Index indexMode;
	private final TermVector termVector;
	private final float boost;
	private final Store storeType;
	private final int precisionStep;
	private final String indexNullAs;

	public LuceneOptionsImpl(DocumentFieldMetadata fieldMetadata) {
		this( fieldMetadata, fieldMetadata.getBoost() );
	}

	public LuceneOptionsImpl(DocumentFieldMetadata fieldMetadata, float boost) {
		this.indexMode = fieldMetadata.getIndex();
		this.termVector = fieldMetadata.getTermVector();
		this.boost = boost;
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

	@Override
	public void addNumericFieldToDocument(String fieldName, Object value, Document document) {
		if ( storeType == Store.COMPRESS ) {
			throw new SearchException( "Error indexing field " + fieldName + ", @NumericField cannot be compressed" );
		}
		if ( value != null ) {
			NumericField numericField = new NumericField(
					fieldName, precisionStep, storeType != Store.NO ? Field.Store.YES : Field.Store.NO, true
			);
			NumericFieldUtils.setNumericValue( value, numericField );
			numericField.setBoost( boost );

			if ( numericField.getNumericValue() != null ) {
				document.add( numericField );
			}
		}
	}

	private void standardFieldAdd(String name, String indexedString, Document document) {
		Field field = new Field(
				name, true, indexedString, storeUncompressed ? Field.Store.YES : Field.Store.NO, indexMode, termVector
		);
		field.setBoost( boost );
		document.add( field );
	}

	private void compressedFieldAdd(String name, String indexedString, Document document) {
		byte[] compressedString = CompressionTools.compressString( indexedString );
		// indexed is implicitly set to false when using byte[]
		Field field = new Field( name, compressedString );
		document.add( field );
	}

	@Override
	public float getBoost() {
		return boost;
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
}
