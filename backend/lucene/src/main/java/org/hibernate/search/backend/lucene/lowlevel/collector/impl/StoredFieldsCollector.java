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
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.ChildDocIds;
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

	public static final CollectorKey<StoredFieldsCollector> KEY = CollectorKey.create();

	public static CollectorFactory<StoredFieldsCollector> factory(
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			Set<String> requiredNestedDocumentPathsForStoredFields) {
		return new CollectorFactory<StoredFieldsCollector>() {
			@Override
			public StoredFieldsCollector createCollector(CollectorExecutionContext context) throws IOException {
				NestedDocsProvider nestedDocsProvider;
				if ( requiredNestedDocumentPathsForStoredFields.isEmpty() ) {
					nestedDocsProvider = null;
				}
				else {
					nestedDocsProvider = context.createNestedDocsProvider( requiredNestedDocumentPathsForStoredFields );
				}

				return new StoredFieldsCollector( nestedDocsProvider, storedFieldVisitor, context.getIndexSearcher() );
			}

			@Override
			public CollectorKey<StoredFieldsCollector> getCollectorKey() {
				return KEY;
			}
		};
	}

	private final NestedDocsProvider nestedDocsProvider;
	private final Weight childrenWeight;
	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;

	private int currentLeafDocBase;
	private int currentLeafLastSeenParentDoc;
	private ChildDocIds currentLeafChildDocs;
	private LeafReader currentLeafReader;

	private final Map<Integer, Document> documents = new HashMap<>();

	public StoredFieldsCollector(NestedDocsProvider nestedDocsProvider,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			IndexSearcher indexSearcher) throws IOException {
		this.childrenWeight = nestedDocsProvider == null ? null : nestedDocsProvider.childDocsWeight( indexSearcher );
		this.nestedDocsProvider = nestedDocsProvider;
		this.storedFieldVisitor = storedFieldVisitor;
	}

	@Override
	public String toString() {
		return "StoredFieldsCollector{" + "documents=" + documents + '}';
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
		currentLeafLastSeenParentDoc = parentDoc;

		if ( !currentLeafChildDocs.advanceExactParent( parentDoc ) ) {
			// No child
			return;
		}

		for ( int childDoc = currentLeafChildDocs.nextChild(); childDoc != DocIdSetIterator.NO_MORE_DOCS;
				childDoc = currentLeafChildDocs.nextChild() ) {
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

		this.currentLeafChildDocs = nestedDocsProvider == null ? null
				: nestedDocsProvider.childDocs( childrenWeight, context, null );
	}
}
