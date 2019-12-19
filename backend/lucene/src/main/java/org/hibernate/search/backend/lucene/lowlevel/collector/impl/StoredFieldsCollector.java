/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Weight;

/**
 * Collects stored fields as Document instances.
 * <p>
 * <strong>WARNING:</strong> this relies on reader.document() to load the value of stored field
 * for <strong>each single matching document</strong>,
 * Use with care.
 */
public class StoredFieldsCollector extends SimpleCollector {

	private final NestedDocsProvider nestedDocsProvider;
	private final Weight childrenWeight;
	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;

	private int currentLeafDocBase;
	private int currentLeafLastSeenParentDoc;
	private DocIdSetIterator currentLeafChildDocs;
	private LeafReader currentLeafReader;

	private final Map<Integer, Document> documents = new HashMap<>();

	public StoredFieldsCollector(IndexSearcher indexSearcher, NestedDocsProvider nestedDocsProvider,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor) throws IOException {
		this.childrenWeight = nestedDocsProvider == null ? null : nestedDocsProvider.childDocsWeight( indexSearcher );
		this.nestedDocsProvider = nestedDocsProvider;
		this.storedFieldVisitor = storedFieldVisitor;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "ChildrenCollector{" );
		sb.append( "documents=" ).append( documents );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public void collect(int parentDoc) throws IOException {
		// add nested documents contribution
		if ( currentLeafChildDocs != null ) {
			collectChildDocs( parentDoc );
		}

		// add root document contribution
		currentLeafReader.document( parentDoc, storedFieldVisitor );

		documents.put( currentLeafDocBase + parentDoc, storedFieldVisitor.getDocumentAndReset() );
	}

	private void collectChildDocs(int parentDoc) throws IOException {
		if ( parentDoc < currentLeafLastSeenParentDoc ) {
			throw new AssertionFailure( "Collector.collect called in unexpected order" );
		}

		final int firstChildDoc;
		if ( currentLeafChildDocs.docID() > currentLeafLastSeenParentDoc ) {
			firstChildDoc = currentLeafChildDocs.docID();
		}
		else {
			firstChildDoc = currentLeafChildDocs.advance( currentLeafLastSeenParentDoc + 1 );
		}
		currentLeafLastSeenParentDoc = parentDoc;

		if ( firstChildDoc > parentDoc ) {
			// No child
			return;
		}

		for ( int childDoc = firstChildDoc; childDoc < parentDoc; childDoc = currentLeafChildDocs.nextDoc() ) {
			currentLeafReader.document( childDoc, storedFieldVisitor );
		}
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	public Document getDocument(int docId) {
		return documents.get( docId );
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.currentLeafDocBase = context.docBase;
		this.currentLeafLastSeenParentDoc = -1;
		this.currentLeafReader = context.reader();

		this.currentLeafChildDocs = nestedDocsProvider == null ? null : nestedDocsProvider.childDocs( childrenWeight, context );
	}
}
