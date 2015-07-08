/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
	private final int totalFields;

	//The Lucene Document which will be returned. Lazily initialized.
	private Document doc = null;

	//Counts the number of fields which have not been loaded yet (counting down from the known set of needed fields)
	//This field needs to be reset to the value of totalFields when the doc field is changed.
	private int missingFields;

	public ReusableDocumentStoredFieldVisitor(Set<String> fieldsToLoad) {
		FieldAcceptor previous = NOT_ACCEPT;
		for ( String fieldName : fieldsToLoad ) {
			previous = new ChainedFieldAcceptor( previous, fieldName );
		}
		this.rootAcceptor = previous;
		this.totalFields = fieldsToLoad.size();
		this.missingFields = totalFields;
	}

	@Override
	public void binaryField(FieldInfo fieldInfo, byte[] value) throws IOException {
		getDocument().add( new StoredField( fieldInfo.name, value ) );
	}

	@Override
	public void stringField(FieldInfo fieldInfo, byte[] value) throws IOException {
		final FieldType ft = new FieldType( TextField.TYPE_STORED );
		ft.setStoreTermVectors( fieldInfo.hasVectors() );
		ft.setOmitNorms( fieldInfo.omitsNorms() );
		ft.setIndexOptions( fieldInfo.getIndexOptions() );
		getDocument().add( new Field( fieldInfo.name, new String( value, StandardCharsets.UTF_8 ), ft ) );
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
		if ( missingFields == 0 ) {
			// An aggressive STOP could prevent unnecessary I/O !
			return Status.STOP;
		}
		final Status s = rootAcceptor.acceptField( fieldInfo.name );
		if ( s == Status.YES ) {
			missingFields--;
		}
		return s;
	}

	/**
	 * Useful for tests
	 * @return the amount of accepted fields
	 */
	public int countAcceptedFields() {
		FieldAcceptor acceptor = rootAcceptor;
		int count = 0;
		while ( acceptor != NOT_ACCEPT ) {
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
			this.missingFields = totalFields;
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
