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
package org.hibernate.search.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.max;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

/**
 * A DocIdSet built as applying "AND" operation to a list of other DocIdSet(s).
 * The DocIdSetIterator returned will return only document ids contained
 * in all DocIdSet(s) handed to the constructor.
 * 
 * @author Sanne Grinovero
 */
public class AndDocIdSet extends DocIdSet {
	
	private DocIdSet docIdBitSet;
	private final List<DocIdSet> andedDocIdSets;
	private final int maxDocNumber;
	
	public AndDocIdSet(List<DocIdSet> andedDocIdSets, int maxDocs) {
		if ( andedDocIdSets == null || andedDocIdSets.size() < 2 )
			throw new IllegalArgumentException( "To \"and\" some DocIdSet(s) they should be at least 2" );
		this.andedDocIdSets = new ArrayList<DocIdSet>( andedDocIdSets ); // make a defensive mutable copy
		this.maxDocNumber = maxDocs;
	}
	
	private synchronized DocIdSet buildBitset() throws IOException {
		if ( docIdBitSet != null ) return docIdBitSet; // check for concurrent initialization
		//TODO if all andedDocIdSets are actually DocIdBitSet, use their internal BitSet instead of next algo.
		//TODO if some andedDocIdSets are DocIdBitSet, merge them first.
		int size = andedDocIdSets.size();
		DocIdSetIterator[] iterators = new DocIdSetIterator[size];
		for (int i=0; i<size; i++) {
			// build all iterators
			iterators[i] = andedDocIdSets.get(i).iterator();
		}
		andedDocIdSets.clear(); // contained DocIdSets are not needed any more, release them.
		docIdBitSet = makeDocIdSetOnAgreedBits( iterators ); // before returning hold a copy as cache
		return docIdBitSet;
	}

	private final DocIdSet makeDocIdSetOnAgreedBits(final DocIdSetIterator[] iterators) throws IOException {
		final int iteratorSize = iterators.length;
		int targetPosition = Integer.MIN_VALUE;
		int votes = 0;
		// Each iterator can vote "ok" for the current target to
		// be reached; when all agree the bit is set.
		// if an iterator disagrees (it jumped longer), it's current position becomes the new targetPosition
		// for the others and he is considered "first" in the voting round (every iterator votes for himself ;-)
		int i = 0;
		//iterator initialize, just one "next" for each DocIdSetIterator
		for ( ; i<iteratorSize; i++ ) {
			final DocIdSetIterator iterator = iterators[i];
			final int position = iterator.nextDoc();
			if ( position==DocIdSetIterator.NO_MORE_DOCS ) {
				//current iterator has no values, so skip all
				return DocIdSet.EMPTY_DOCIDSET;
			}
			if ( targetPosition==position ) {
				votes++; //stopped as same position of others
			}
			else {
				targetPosition = max( targetPosition, position );
				if (targetPosition==position) //means it changed
					votes=1;
			}
		}
		final OpenBitSet result = new OpenBitSet( maxDocNumber );
		// end iterator initialize
		if (votes==iteratorSize) {
			result.fastSet( targetPosition );
			targetPosition++;
		}
		i = 0;
		votes = 0; //could be smarter but would make the code even more complex for a minor optimization out of cycle.
		// enter main loop:
		while ( true ) {
			final DocIdSetIterator iterator = iterators[i];
			final int position = iterator.advance( targetPosition );
			if ( position==DocIdSetIterator.NO_MORE_DOCS )
				return result; //exit condition
			if ( position == targetPosition ) {
				if ( ++votes == iteratorSize ) {
					result.fastSet( position );
					votes = 0;
					targetPosition++;
				}
			}
			else {
				votes = 1;
				targetPosition = position;
			}
			i = ++i % iteratorSize;
		}
	}

	@Override
	public DocIdSetIterator iterator() throws IOException {
		return buildBitset().iterator();
	}
	
	@Override
	public boolean isCacheable() {
		return true;
	}
	
}
