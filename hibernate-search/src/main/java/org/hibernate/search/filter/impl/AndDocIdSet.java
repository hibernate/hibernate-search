/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.filter.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

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
			// build all iterators
			DocIdSetIterator docIdSetIterator = andedDocIdSets.get( i ).iterator();
			if ( docIdSetIterator == null ) {
				// the Lucene API permits to return null on any iterator for empty matches
				return DocIdSet.EMPTY_DOCIDSET;
			}
			iterators[i] = docIdSetIterator;
		}
		andedDocIdSets.clear(); // contained DocIdSets are not needed any more, release them.
		docIdBitSet = makeDocIdSetOnAgreedBits( iterators ); // before returning hold a copy as cache
		return docIdBitSet;
	}

	private DocIdSet makeDocIdSetOnAgreedBits(final DocIdSetIterator[] iterators) throws IOException {
		final OpenBitSet result = new OpenBitSet( maxDocNumber );
		final int numberOfIterators = iterators.length;

		int targetPosition = findFirstTargetPosition( iterators, result );

		if ( targetPosition == DocIdSetIterator.NO_MORE_DOCS ) {
			return DocIdSet.EMPTY_DOCIDSET;
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
				return result;
			} //exit condition
			if ( position == targetPosition ) {
				if ( ++votes == numberOfIterators ) {
					result.fastSet( position );
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

	private int findFirstTargetPosition(final DocIdSetIterator[] iterators, OpenBitSet result) throws IOException {
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
			result.fastSet( targetPosition );
			targetPosition++;
		}

		return targetPosition;
	}
}
