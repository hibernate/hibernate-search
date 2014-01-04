/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.StoredFieldVisitor;

/**
 * Inspired by Lucene's DocumentStoredFieldVisitor, with the difference that we want
 * to reuse the same FieldVisitor to load multiple Document instances.
 * Also the accepted fields are kept in a linked-list like structure, to optimize
 * for small amounts of accepted fields.
 *
 * A ReusableDocumentStoredFieldVisitor is NOT threadsafe: in case you need one
 * for multiple threads make a clone for each thread.
 *
 * @author Sanne Grinovero
 */
public final class ReusableDocumentStoredFieldVisitor extends StoredFieldVisitor implements Cloneable {

	private static final FieldAcceptor NEGATIVE_ACCEPT = new NegativeFieldAcceptor();

	private final FieldAcceptor rootAcceptor;
	private Document doc = null;

	public ReusableDocumentStoredFieldVisitor(Set<String> fieldsToLoad) {
		FieldAcceptor previous = NEGATIVE_ACCEPT;
		for ( String fieldName : fieldsToLoad ) {
			previous = new ChainedFieldAcceptor( previous, fieldName );
		}
		this.rootAcceptor = previous;
	}

	private ReusableDocumentStoredFieldVisitor(FieldAcceptor rootAcceptor) {
		this.rootAcceptor = rootAcceptor;
	}

	@Override
	public void binaryField(FieldInfo fieldInfo, byte[] value) throws IOException {
		getDocument().add( new StoredField( fieldInfo.name, value ) );
	}

	@Override
	public void stringField(FieldInfo fieldInfo, String value) throws IOException {
		final FieldType ft = new FieldType( TextField.TYPE_STORED );
		ft.setStoreTermVectors( fieldInfo.hasVectors() );
		ft.setIndexed( fieldInfo.isIndexed() );
		ft.setOmitNorms( fieldInfo.omitsNorms() );
		ft.setIndexOptions( fieldInfo.getIndexOptions() );
		getDocument().add( new Field( fieldInfo.name, value, ft ) );
	}

	@Override
	public void intField(FieldInfo fieldInfo, int value) {
		getDocument().add( new StoredField( fieldInfo.name, value ) );
	}

	@Override
	public void longField(FieldInfo fieldInfo, long value) {
		getDocument().add( new StoredField( fieldInfo.name, value ) );
	}

	@Override
	public void floatField(FieldInfo fieldInfo, float value) {
		getDocument().add( new StoredField( fieldInfo.name, value ) );
	}

	@Override
	public void doubleField(FieldInfo fieldInfo, double value) {
		getDocument().add( new StoredField( fieldInfo.name, value ) );
	}

	@Override
	public Status needsField(FieldInfo fieldInfo) throws IOException {
		return rootAcceptor.acceptField( fieldInfo.name );
	}

	/**
	 * Useful for tests
	 * @return the amount of accepted fields
	 */
	public int countAcceptedFields() {
		FieldAcceptor acceptor = rootAcceptor;
		int count = 0;
		while ( acceptor != NEGATIVE_ACCEPT ) {
			//If it's not negative it has to be positive:
			ChainedFieldAcceptor positiveAcceptor = (ChainedFieldAcceptor) acceptor;
			acceptor = positiveAcceptor.next;
			count++;
		}
		return count;
	}

	/**
	 * Retrieve the visited document, and resets the instance to be reused by creating a new Document
	 * internally.
	 *
	 * @return Document populated with stored fields.
	 */
	public Document getDocumentAndReset() {
		final Document localDoc = this.doc;
		if ( localDoc == null ) {
			return new Document();
		}
		else {
			this.doc = null;
			return localDoc;
		}
	}

	private Document getDocument() {
		Document localDoc = this.doc;
		if ( localDoc == null ) {
			localDoc = new Document();
			this.doc = localDoc;
		}
		return localDoc;
	}

	@Override
	public ReusableDocumentStoredFieldVisitor clone() {
		return new ReusableDocumentStoredFieldVisitor( rootAcceptor );
	}

	/* The structure below shapes a chain of accepted field names:
	 * you could think of it as a linked list. */

	private interface FieldAcceptor {
		Status acceptField(String fieldName);
	}

	private static final class NegativeFieldAcceptor implements FieldAcceptor {
		public Status acceptField(final String fieldName) {
			return Status.NO;
		}
	}

	private static final class ChainedFieldAcceptor implements FieldAcceptor {
		final FieldAcceptor next;
		final String acceptedFieldName;
		ChainedFieldAcceptor(FieldAcceptor next, String acceptedFieldName) {
			this.next = next;
			this.acceptedFieldName = acceptedFieldName;
		}
		public Status acceptField(final String fieldName) {
			if ( acceptedFieldName.equals( fieldName ) ) {
				return Status.YES;
			}
			else {
				return next.acceptField( fieldName );
			}
		}
	}

}
