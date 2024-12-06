/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.ChildDocIds;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.util.common.AssertionFailure;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;

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
	private final IntObjectMap<Document> currentChildDocValues;

	public StoredFieldsValuesDelegate(NestedDocsProvider nestedDocsProvider,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			IndexSearcher indexSearcher)
			throws IOException {
		this.childrenWeight = nestedDocsProvider == null ? null : nestedDocsProvider.childDocsWeight( indexSearcher );
		this.nestedDocsProvider = nestedDocsProvider;
		this.storedFieldVisitor = storedFieldVisitor;
		this.currentChildDocValues = nestedDocsProvider == null ? null : new IntObjectHashMap<>();
	}

	@Override
	public String toString() {
		return "StoredFieldsValues{" +
				"storedFieldVisitor=" + storedFieldVisitor +
				'}';
	}

	void context(LeafReaderContext context) throws IOException {
		this.currentLeafReader = context.reader();
		this.currentLeafChildDocs = nestedDocsProvider == null
				? null
				: nestedDocsProvider.childDocs( childrenWeight, context, null );

		this.currentRootDoc = -1;
		this.currentRootDocValue = null;
		if ( currentChildDocValues != null ) {
			this.currentChildDocValues.clear();
		}
	}

	void collect(int parentDoc) throws IOException {
		this.currentRootDoc = parentDoc;

		// collect child documents if necessary
		if ( currentLeafChildDocs != null && currentLeafChildDocs.advanceExactParent( parentDoc ) ) {
			for ( int childDoc = currentLeafChildDocs.nextChild(); childDoc != DocIdSetIterator.NO_MORE_DOCS;
					childDoc = currentLeafChildDocs.nextChild() ) {
				currentLeafReader.storedFields().document( childDoc, storedFieldVisitor );
				currentChildDocValues.put( childDoc, storedFieldVisitor.getDocumentAndReset() );
			}
		}

		// collect root document
		currentLeafReader.storedFields().document( parentDoc, storedFieldVisitor );
		this.currentRootDocValue = storedFieldVisitor.getDocumentAndReset();
	}

	public Document get(int docId) {
		if ( docId == currentRootDoc ) {
			return currentRootDocValue;
		}
		Document doc = currentChildDocValues.get( docId );
		if ( doc == null ) {
			throw new AssertionFailure( "Getting value for " + docId + ", which is neither root document "
					+ currentRootDoc + " nor children " + currentChildDocValues.keys() );
		}
		return doc;
	}

}
