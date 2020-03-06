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
 * An iterator on doc IDs returning for each parent document the docID of the first child document with a value.
 * <p>
 * This iterator has side-effects: iterators passed to the constructor of this class
 * may advance when {@link #advance(int)} is called on this iterator.
 */
public class JoinFirstChildIdIterator {

	public static final int NO_CHILD_WITH_VALUE = -1;

	private final BitSet parentDocs;
	private final DocIdSetIterator childDocs;
	private final DocIdSetIterator values;

	private int lastSeenParentDocId = -1;
	private int childDocId = -1;

	public JoinFirstChildIdIterator(BitSet parentDocs, DocIdSetIterator childDocs,
			DocIdSetIterator values) {
		this.parentDocs = parentDocs;
		this.childDocs = childDocs;
		this.values = values;
	}

	/**
	 * @param parentDocId The docID of a parent document. Must be greater than or equal to
	 * the docId passed to this method the last time it was invoked.
	 * @return The docID of the first child of this parent with a value, or {@link #NO_CHILD_WITH_VALUE} if there is none.
	 * @throws IOException If advancing underlying iterators throws an exception.
	 */
	public int advance(int parentDocId) throws IOException {
		if ( parentDocId == lastSeenParentDocId ) {
			return childDocId;
		}

		int prevParentDoc = parentDocs.prevSetBit( parentDocId - 1 );

		int candidateDocId;

		// Find the next child of this parent or a later parent
		if ( childDocs.docID() > prevParentDoc ) { // Strict comparison: the previous parent is not a child.
			candidateDocId = childDocs.docID();
		}
		else {
			candidateDocId = childDocs.advance( prevParentDoc + 1 );
		}

		// Find the next document with a value, greater than or equal to the child doc ID.
		if ( values.docID() >= candidateDocId ) { // Non-strict comparison: if values are already on the candidate, we'll take it.
			candidateDocId = values.docID();
		}
		else {
			candidateDocId = values.advance( candidateDocId );
		}

		lastSeenParentDocId = parentDocId;
		if ( candidateDocId < parentDocId ) {
			childDocId = candidateDocId;
		}
		else {
			childDocId = NO_CHILD_WITH_VALUE;
		}

		return childDocId;
	}

}
