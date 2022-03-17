/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
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
import org.apache.lucene.search.Weight;

/**
 * Collects values from stored fields, for use in {@link Values} implementations.
 * <p>
 * <strong>WARNING:</strong> this relies on reader.document() to load the value of stored field
 * for <strong>each single matching document</strong>,
 * Use with care.
 */
public class StoredFieldsValuesDelegate {
	public static class Factory {
		private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
		private final Set<String> requiredNestedDocumentPathsForStoredFields;

		public Factory(ReusableDocumentStoredFieldVisitor storedFieldVisitor,
				Set<String> requiredNestedDocumentPathsForStoredFields) {

			this.storedFieldVisitor = storedFieldVisitor;
			this.requiredNestedDocumentPathsForStoredFields = requiredNestedDocumentPathsForStoredFields;
		}

		public StoredFieldsValuesDelegate create(CollectorExecutionContext context) throws IOException {
			NestedDocsProvider nestedDocsProvider;
			if ( requiredNestedDocumentPathsForStoredFields.isEmpty() ) {
				nestedDocsProvider = null;
			}
			else {
				nestedDocsProvider = context.createNestedDocsProvider( requiredNestedDocumentPathsForStoredFields );
			}

			return new StoredFieldsValuesDelegate( nestedDocsProvider, storedFieldVisitor, context.getIndexSearcher() );
		}
	}

	private final NestedDocsProvider nestedDocsProvider;
	private final Weight childrenWeight;
	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;

	private ChildDocIds currentLeafChildDocs;
	private LeafReader currentLeafReader;

	private int currentRootDoc;
	private Document currentRootDocValue;

	public StoredFieldsValuesDelegate(NestedDocsProvider nestedDocsProvider,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			IndexSearcher indexSearcher) throws IOException {
		this.childrenWeight = nestedDocsProvider == null ? null : nestedDocsProvider.childDocsWeight( indexSearcher );
		this.nestedDocsProvider = nestedDocsProvider;
		this.storedFieldVisitor = storedFieldVisitor;
	}

	@Override
	public String toString() {
		return "StoredFieldsValues{" +
				"storedFieldVisitor=" + storedFieldVisitor +
				'}';
	}

	void context(LeafReaderContext context) throws IOException {
		this.currentLeafReader = context.reader();
		this.currentLeafChildDocs = nestedDocsProvider == null ? null
				: nestedDocsProvider.childDocs( childrenWeight, context, null );

		this.currentRootDoc = -1;
		this.currentRootDocValue = null;
	}

	void collect(int parentDoc) throws IOException {
		this.currentRootDoc = parentDoc;

		// collect child documents if necessary
		if ( currentLeafChildDocs != null && currentLeafChildDocs.advanceExactParent( parentDoc ) ) {
			for ( int childDoc = currentLeafChildDocs.nextChild(); childDoc != DocIdSetIterator.NO_MORE_DOCS;
					childDoc = currentLeafChildDocs.nextChild() ) {
				currentLeafReader.document( childDoc, storedFieldVisitor );
			}
		}

		// collect root document
		currentLeafReader.document( parentDoc, storedFieldVisitor );

		this.currentRootDocValue = storedFieldVisitor.getDocumentAndReset();
	}

	public Document get(int doc) {
		if ( doc != currentRootDoc ) {
			throw new AssertionFailure( "Getting value for " + doc + ", but current root document is "
					+ currentRootDoc );
		}
		return currentRootDocValue;
	}

}
