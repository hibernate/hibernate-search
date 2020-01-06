/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import org.apache.lucene.search.DocIdSetIterator;

final class ExplicitDocIdSetIterator extends DocIdSetIterator {

	static DocIdSetIterator of(int[] sortedTopLevelDocIds, int leafDocBase, int leafMaxLeafId) {
		int firstIndex = findFirstGreaterThanOrEqualTo( sortedTopLevelDocIds, leafDocBase );
		if ( firstIndex < 0 ) {
			return DocIdSetIterator.empty();
		}
		else {
			return new ExplicitDocIdSetIterator( sortedTopLevelDocIds, leafDocBase, firstIndex, leafMaxLeafId );
		}
	}

	private static int findFirstGreaterThanOrEqualTo(int[] values, int min) {
		for ( int i = 0; i < values.length; i++ ) {
			if ( values[i] >= min ) {
				return i;
			}
		}
		return -1;
	}

	private final int[] sortedTopLevelDocIds;
	private final int leafDocBase;
	private final int firstIndex;
	private final int leafMaxDocId;

	private int index;
	private int leafDocId = -1;

	ExplicitDocIdSetIterator(int[] sortedTopLevelDocIds, int leafDocBase, int firstIndex, int leafMaxDocId) {
		this.sortedTopLevelDocIds = sortedTopLevelDocIds;
		this.leafDocBase = leafDocBase;
		this.firstIndex = firstIndex;
		this.leafMaxDocId = leafMaxDocId;

		this.index = firstIndex;
	}

	@Override
	public int docID() {
		return leafDocId;
	}

	@Override
	public int nextDoc() {
		if ( index < sortedTopLevelDocIds.length ) {
			// Subtract docBase to convert from top-level doc id to leaf docId
			leafDocId = sortedTopLevelDocIds[index] - leafDocBase;
			++index;
			if ( leafDocId < leafMaxDocId ) {
				return leafDocId;
			}
		}

		leafDocId = NO_MORE_DOCS;
		return leafDocId;
	}

	@Override
	public int advance(int target) {
		int doc;
		while ( ( doc = nextDoc() ) < target ) {
			// Nothing to do: nextDoc() advances the iterator.
			// Performance is acceptable since we don't expect there will be many doc IDs.
		}
		return doc;
	}

	@Override
	public long cost() {
		return (long) sortedTopLevelDocIds.length - firstIndex;
	}
}
