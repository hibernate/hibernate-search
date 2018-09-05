/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.FixedBitSet;

import static java.lang.Math.max;

/**
 * A DocIdSet built as applying "AND" operation to a list of other DocIdSet(s).
 * The DocIdSetIterator returned will return only document ids contained
 * in all DocIdSet(s) handed to the constructor.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class AndDocIdSet extends DocIdSet {

	private DocIdSet docIdBitSet;
	private final List<DocIdSet> andedDocIdSets;
	private final int maxDocNumber;

	public AndDocIdSet(List<DocIdSet> andedDocIdSets, int maxDocs) {
		if ( andedDocIdSets == null || andedDocIdSets.size() < 2 ) {
			throw new IllegalArgumentException( "To \"and\" some DocIdSet(s) they should be at least 2" );
		}
		this.andedDocIdSets = new ArrayList<DocIdSet>( andedDocIdSets ); // make a defensive mutable copy
		this.maxDocNumber = maxDocs;
	}

	@Override
	public DocIdSetIterator iterator() throws IOException {
		return buildBitSet().iterator();
	}

	@Override
	public boolean isCacheable() {
		return true;
	}

	private synchronized DocIdSet buildBitSet() throws IOException {
		if ( docIdBitSet != null ) {
			return docIdBitSet;
		} // check for concurrent initialization
		//TODO if all andedDocIdSets are actually DocIdBitSet, use their internal BitSet instead of next algo.
		//TODO if some andedDocIdSets are DocIdBitSet, merge them first.
		int size = andedDocIdSets.size();
		DocIdSetIterator[] iterators = new DocIdSetIterator[size];
		for ( int i = 0; i < size; i++ ) {
			DocIdSet docIdSet = andedDocIdSets.get( i );
			if ( docIdSet == null ) {
				// Since Lucene 4 even the docIdSet could be returned at null to signify an empty match
				return EMPTY_DOCIDSET;
			}
			// build all iterators
			DocIdSetIterator docIdSetIterator = docIdSet.iterator();
			if ( docIdSetIterator == null ) {
				// the Lucene API permits to return null on any iterator for empty matches
				return EMPTY_DOCIDSET;
			}
			iterators[i] = docIdSetIterator;
		}
		andedDocIdSets.clear(); // contained DocIdSets are not needed any more, release them.
		docIdBitSet = makeDocIdSetOnAgreedBits( iterators ); // before returning hold a copy as cache
		return docIdBitSet;
	}

	private DocIdSet makeDocIdSetOnAgreedBits(final DocIdSetIterator[] iterators) throws IOException {
		final FixedBitSet result = new FixedBitSet( maxDocNumber );
		final int numberOfIterators = iterators.length;

		int targetPosition = findFirstTargetPosition( iterators, result );

		if ( targetPosition == DocIdSetIterator.NO_MORE_DOCS ) {
			return EMPTY_DOCIDSET;
		}

		// Each iterator can vote "ok" for the current target to
		// be reached; when all agree the bit is set.
		// if an iterator disagrees (it jumped longer), it's current position becomes the new targetPosition
		// for the others and he is considered "first" in the voting round (every iterator votes for himself ;-)

		int i = 0;
		int votes = 0; //could be smarter but would make the code even more complex for a minor optimization out of cycle.
		// enter main loop:
		while ( true ) {
			final DocIdSetIterator iterator = iterators[i];
			int position = targetPosition;
			if ( !iteratorAlreadyOnTargetPosition( targetPosition, iterator ) ) {
				position = iterator.advance( targetPosition );
			}
			if ( position == DocIdSetIterator.NO_MORE_DOCS ) {
				return new BitDocIdSet( result );
			} //exit condition
			if ( position == targetPosition ) {
				if ( ++votes == numberOfIterators ) {
					result.set( position );
					votes = 0;
					targetPosition++;
				}
			}
			else {
				votes = 1;
				targetPosition = position;
			}
			i = ++i % numberOfIterators;
		}
	}

	// see  HSEARCH-610
	private boolean iteratorAlreadyOnTargetPosition(int targetPosition, DocIdSetIterator iterator) {
		return iterator.docID() == targetPosition;
	}

	private int findFirstTargetPosition(final DocIdSetIterator[] iterators, FixedBitSet result) throws IOException {
		int targetPosition = iterators[0].nextDoc();
		if ( targetPosition == DocIdSetIterator.NO_MORE_DOCS ) {
			// first iterator has no values, so skip all
			return DocIdSetIterator.NO_MORE_DOCS;
		}

		boolean allIteratorsShareSameFirstTarget = true;

		//iterator initialize, just one "next" for each DocIdSetIterator
		for ( int i = 1; i < iterators.length; i++ ) {
			final DocIdSetIterator iterator = iterators[i];
			final int position = iterator.nextDoc();
			if ( position == DocIdSetIterator.NO_MORE_DOCS ) {
				//current iterator has no values, so skip all
				return DocIdSetIterator.NO_MORE_DOCS;
			}
			if ( targetPosition != position ) {
				targetPosition = max( targetPosition, position );
				allIteratorsShareSameFirstTarget = false;
			}
		}
		// end iterator initialize

		if ( allIteratorsShareSameFirstTarget ) {
			result.set( targetPosition );
			targetPosition++;
		}

		return targetPosition;
	}

	public static final DocIdSet EMPTY_DOCIDSET = DocIdSet.EMPTY;

	/* (non-Javadoc)
	 * @see org.apache.lucene.util.Accountable#ramBytesUsed()
	 */
	@Override
	public long ramBytesUsed() {
		long memoryEstimate = docIdBitSet.ramBytesUsed();
		for ( DocIdSet sets : andedDocIdSets ) {
			memoryEstimate += sets.ramBytesUsed();
		}
		return memoryEstimate;
	}

}
