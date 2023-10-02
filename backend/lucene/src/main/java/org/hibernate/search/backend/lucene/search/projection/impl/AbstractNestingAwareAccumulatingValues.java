/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.ChildDocIds;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;

abstract class AbstractNestingAwareAccumulatingValues<E, A> implements Values<A> {
	private final NestedDocsProvider nestedDocsProvider;
	protected final ProjectionAccumulator<E, ?, A, ?> accumulator;

	protected ChildDocIds currentLeafChildDocIds;

	AbstractNestingAwareAccumulatingValues(String parentDocumentPath, String nestedDocumentPath,
			ProjectionAccumulator<E, ?, A, ?> accumulator, TopDocsDataCollectorExecutionContext context) {
		this.nestedDocsProvider = nestedDocumentPath == null || nestedDocumentPath.equals( parentDocumentPath )
				? null
				: context.createNestedDocsProvider( parentDocumentPath, nestedDocumentPath );
		this.accumulator = accumulator;
	}

	@Override
	public void context(LeafReaderContext context) throws IOException {
		DocIdSetIterator valuesOrNull = doContext( context );
		if ( nestedDocsProvider != null ) {
			currentLeafChildDocIds = nestedDocsProvider.childDocs( context, valuesOrNull );
		}
	}

	protected DocIdSetIterator doContext(LeafReaderContext context) throws IOException {
		// Nothing to do; to be overridden if necessary.
		return null;
	}

	@Override
	public final A get(int parentDocId) throws IOException {
		A accumulated = accumulator.createInitial();

		if ( nestedDocsProvider == null ) {
			// No nesting: we work directly on the parent doc.
			accumulated = accumulate( accumulated, parentDocId );
			return accumulated;
		}
		if ( currentLeafChildDocIds == null ) {
			// No child documents, hence no values, in the current leaf.
			return accumulated;
		}

		if ( !currentLeafChildDocIds.advanceExactParent( parentDocId ) ) {
			return accumulated;
		}

		for ( int currentChildDocId = currentLeafChildDocIds.nextChild();
				currentChildDocId != DocIdSetIterator.NO_MORE_DOCS;
				currentChildDocId = currentLeafChildDocIds.nextChild() ) {
			accumulated = accumulate( accumulated, currentChildDocId );
		}

		return accumulated;
	}

	protected abstract A accumulate(A accumulated, int docId) throws IOException;

}
