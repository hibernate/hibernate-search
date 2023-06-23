/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.join.impl;

import java.io.IOException;

import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;

/**
 * A provider of children docIds for a given parent docId.
 */
public class ChildDocIds {

	private final BitSet parentDocs;
	private final DocIdSetIterator childDocs;

	private int currentParentDocId = -1;
	private int lastReturnedChildDocId = -1;

	public ChildDocIds(BitSet parentDocs, DocIdSetIterator childDocs) {
		this.parentDocs = parentDocs;
		this.childDocs = childDocs;
	}

	/**
	 * @param parentDocId The docID of a parent document. Must be strictly greater than
	 * the docId passed to this method the last time it was invoked.
	 * @return {@code true} if there is at least one child for the given parent.
	 * @throws IOException If advancing underlying iterators throws an exception.
	 */
	public boolean advanceExactParent(int parentDocId) throws IOException {
		if ( parentDocId <= currentParentDocId ) {
			throw new AssertionFailure(
					"This iterator can only move forward (no advancing to the same doc twice, no going backward)" );
		}
		currentParentDocId = parentDocId;
		lastReturnedChildDocId = -1;

		int prevParentDocId = parentDocId == 0 ? -1 : parentDocs.prevSetBit( parentDocId - 1 );
		int firstChildDocId = nextChild( prevParentDocId + 1 );
		if ( firstChildDocId < currentParentDocId ) {
			// Set up lastReturnedChildDocId so that the first call to nextChild()
			// will just return firstChildDocId.
			lastReturnedChildDocId = -1;
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @return The docId of the next child of the parent doc set by the last call to {@link #advanceExactParent(int)},
	 * or {@link DocIdSetIterator#NO_MORE_DOCS} if there isn't any more child for this parent doc.
	 * @throws IOException If advancing underlying iterators throws an exception.
	 */
	public int nextChild() throws IOException {
		if ( lastReturnedChildDocId == DocIdSetIterator.NO_MORE_DOCS ) {
			// Avoid integer overflow
			return DocIdSetIterator.NO_MORE_DOCS;
		}
		return nextChild( lastReturnedChildDocId + 1 );
	}

	/**
	 * @param target The docID of a document. Must be greater than or equal to the docId of the last retrieved child.
	 * @return The docId of the next child of the parent doc set by the last call to {@link #advanceExactParent(int)},
	 * beyond {@code previousIdExcluded},
	 * or {@link DocIdSetIterator#NO_MORE_DOCS} if there isn't any more child for this parent doc
	 * beyond {@code previousIdExcluded}.
	 * @throws IOException If advancing underlying iterators throws an exception.
	 */
	public int nextChild(int target) throws IOException {
		if ( currentParentDocId <= lastReturnedChildDocId || currentParentDocId <= target ) {
			// No more children to return
			lastReturnedChildDocId = DocIdSetIterator.NO_MORE_DOCS;
			return lastReturnedChildDocId;
		}
		int nextChild;
		if ( 0 <= childDocs.docID() && target <= childDocs.docID() ) {
			nextChild = childDocs.docID();
		}
		else {
			nextChild = childDocs.advance( target );
		}
		if ( currentParentDocId <= nextChild ) {
			// The next child is not a child of the current parent.
			nextChild = DocIdSetIterator.NO_MORE_DOCS;
		}
		lastReturnedChildDocId = nextChild;
		return nextChild;
	}

}
