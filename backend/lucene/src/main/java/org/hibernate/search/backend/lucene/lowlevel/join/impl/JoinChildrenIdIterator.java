/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.join.impl;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;

/**
 * An iterator on doc IDs that will advance a "values" DocIdSetIterator
 * to each child of a given parent document.
 */
public class JoinChildrenIdIterator {

	private final BitSet parentDocs;
	private final DocIdSetIterator childDocs;
	private final DocIdSetIterator values;

	private int currentParentDocId = -1;
	private int previousChildDocId = -1;

	public JoinChildrenIdIterator(BitSet parentDocs, DocIdSetIterator childDocs,
			DocIdSetIterator values) {
		this.parentDocs = parentDocs;
		this.childDocs = childDocs;
		this.values = values;
	}

	/**
	 * @param parentDocId The docID of a parent document. Must be greater than or equal to
	 * the docId passed to this method the last time it was invoked.
	 * @return {@code true} if there is a child with a value for the given parent.
	 * @throws IOException If advancing underlying iterators throws an exception.
	 */
	public boolean advanceExact(int parentDocId) throws IOException {
		if ( parentDocId == currentParentDocId ) {
			return values.docID() < currentParentDocId;
		}

		int prevParentDoc = parentDocId == 0 ? -1 : parentDocs.prevSetBit( parentDocId - 1 );

		int nextChildDocId = advanceValuesToNextChildBeyond( prevParentDoc );

		currentParentDocId = parentDocId;
		boolean found;
		if ( nextChildDocId < currentParentDocId ) {
			// We found a child with a value.
			found = true;
			// Set up the previous child doc id so that the first call to advanceValuesToNextChild()
			// will just use values.docId().
			previousChildDocId = nextChildDocId - 1;
		}
		else {
			found = false;
			previousChildDocId = DocIdSetIterator.NO_MORE_DOCS;
		}

		return found;
	}

	/**
	 * @return {@code true} if the values successfully advanced to the next child,
	 * {@code false} if there are no more values for the children of the current parent document.
	 * @throws IOException If advancing underlying iterators throws an exception.
	 */
	public boolean advanceValuesToNextChild() throws IOException {
		if ( currentParentDocId <= previousChildDocId ) {
			// A previous call went beyond the parent.
			return false;
		}

		int nextChildDocId = advanceValuesToNextChildBeyond( previousChildDocId );

		// Update the previous child for the next calls.
		previousChildDocId = nextChildDocId;

		// If this is true, then previousChildDocId < nextChildDocId < currentParentDocId:
		// we found the next child and it's a child of the current parent.
		return nextChildDocId < currentParentDocId;
	}

	private int advanceValuesToNextChildBeyond(int previousIdExcluded) throws IOException {
		if ( previousIdExcluded == DocIdSetIterator.NO_MORE_DOCS ) {
			return DocIdSetIterator.NO_MORE_DOCS;
		}

		// Find the next child document beyond the given ID.
		int currentChildDocId = childDocs.docID();
		if ( childDocs.docID() <= previousIdExcluded ) {
			// Advance the child docs iterator beyond the previous, excluded ID.
			currentChildDocId = childDocs.advance( previousIdExcluded + 1 );
		}

		// Find the next child document with a value.
		int currentValuesDocId = values.docID();
		// This loop will end when we find a child with a value, or when both iterators are exhausted.
		while ( currentValuesDocId != currentChildDocId ) {
			// Advance values to the current child or beyond.
			currentValuesDocId = values.advance( currentChildDocId );
			if ( currentChildDocId < currentValuesDocId ) {
				// The previous child didn't have a value and values went beyond the child.
				// Advance the child docs iterator to the current value or beyond.
				currentChildDocId = childDocs.advance( currentValuesDocId );
			}
		}

		return currentValuesDocId;
	}

}
