/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

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
 * @see org.apache.lucene.document.DocumentStoredFieldVisitor
 * @author Sanne Grinovero
 */
public final class ReusableDocumentStoredFieldVisitor extends StoredFieldVisitor {

	private static final FieldAcceptor NOT_ACCEPT = new DenyingFieldAcceptor();

	private final FieldAcceptor rootAcceptor;

	//The Lucene Document which will be returned. Lazily initialized.
	private Document doc = null;

	/**
	 * Create a visitor that collects all fields.
	 */
	public ReusableDocumentStoredFieldVisitor() {
		this.rootAcceptor = null;
	}

	/**
	 * Create a visitor that collects only some specified fields.
	 */
	public ReusableDocumentStoredFieldVisitor(Set<String> fieldsToLoad) {
		FieldAcceptor previous = NOT_ACCEPT;
		for ( String fieldName : fieldsToLoad ) {
			previous = new ChainedFieldAcceptor( previous, fieldName );
		}
		this.rootAcceptor = previous;
	}

	@Override
	public void binaryField(FieldInfo fieldInfo, byte[] value) {
		getDocument().add( new StoredField( fieldInfo.name, value ) );
	}

	@Override
	public void stringField(FieldInfo fieldInfo, String value) {
		final FieldType ft = new FieldType( TextField.TYPE_STORED );
		ft.setStoreTermVectors( fieldInfo.hasVectors() );
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
	public Status needsField(FieldInfo fieldInfo) {
		if ( rootAcceptor == null ) {
			// We need all fields
			return Status.YES;
		}
		return rootAcceptor.acceptField( fieldInfo.name );
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

	/* The structure below shapes a chain of accepted field names:
	 * you could think of it as a linked list. */

	private interface FieldAcceptor {
		Status acceptField(String fieldName);
	}

	private static final class DenyingFieldAcceptor implements FieldAcceptor {
		@Override
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

		@Override
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
