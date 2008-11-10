package org.hibernate.search.filter;

import java.io.IOException;
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
	
	private OpenBitSet docIdBitSet;
	private final List<DocIdSet> andedDocIdSets;
	
	private final int maxDocNumber;
	
	public AndDocIdSet(List<DocIdSet> andedDocIdSets, int maxDocs) {
		if ( andedDocIdSets == null || andedDocIdSets.size() < 2 )
			throw new IllegalArgumentException( "To \"and\" some DocIdSet(s) they should be at least 2" );
		this.andedDocIdSets = andedDocIdSets;
		this.maxDocNumber = maxDocs;
	}
	
	private synchronized OpenBitSet buildBitset() throws IOException {
		if ( docIdBitSet != null ) return docIdBitSet; // double check for concurrent initialization
		//TODO if all andedDocIdSets are actually DocIdBitSet, use their internal BitSet instead of next algo.
		//TODO if some andedDocIdSets are DocIdBitSet, merge them first.
		int size = andedDocIdSets.size();
		DocIdSetIterator[] iterators = new DocIdSetIterator[size];
		boolean valuesExist = true;
		for (int i=0; i<size; i++) {
			// build all iterators
			iterators[i] = andedDocIdSets.get(i).iterator();
		}
		OpenBitSet bitSet;
		if ( valuesExist ) { // skip further processing if some idSet is empty
			bitSet = new OpenBitSet( maxDocNumber );
			markBitSetOnAgree( iterators, bitSet );
		}
		else {
			bitSet = new OpenBitSet(); //TODO a less expensive "empty"
		}
		docIdBitSet = bitSet;
		return bitSet;
	}

	private final void markBitSetOnAgree(final DocIdSetIterator[] iterators, final OpenBitSet result) throws IOException {
		final int iteratorSize = iterators.length;
		int targetPosition = Integer.MIN_VALUE;
		int votes = 0;
		// Each iterator can vote "ok" for the current target to
		// be reached; when all agree the bit is set.
		// if an iterator disagrees (it jumped longer), it's current position becomes the new targetPosition
		// for the others and he is considered "first" in the voting round (every iterator votes for himself ;-)
		int i = 0;
		//iterator initialize, just one "next" for each DocIdSetIterator
		for ( ;i<iteratorSize; i++ ) {
			final DocIdSetIterator iterator = iterators[i];
			if ( ! iterator.next() ) return; //current iterator has no values, so skip all
			final int position = iterator.doc();
			if ( targetPosition==position ) {
				votes++; //stopped as same position of others
			}
			else {
				targetPosition = max( targetPosition, position );
				if (targetPosition==position) //means it changed
					votes=1;
			}
		}
		// end iterator initialize
		if (votes==iteratorSize) {
			result.fastSet( targetPosition );
			targetPosition++;
		}
		i=0;
		votes=0; //could be smarted but would make the code even more complex for a minor optimization out of cycle.
		// enter main loop:
		while ( true ) {
			final DocIdSetIterator iterator = iterators[i];
			final boolean validPosition = iterator.skipTo( targetPosition );
			if ( ! validPosition )
				return; //exit condition
			final int position = iterator.doc();
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
	public DocIdSetIterator iterator() {
		return new AndingDocIdSetIterator();
	}
	
	private class AndingDocIdSetIterator extends DocIdSetIterator {

		private DocIdSetIterator iterator;

		@Override
		public int doc() {
			// should never happen when respecting interface contract; otherwise I
			// prefer a NPE than a hard to debug return 0.
			assert iterator != null : "Illegal state, can't be called before next() or skipTo(int)";
			return iterator.doc();
		}

		@Override
		public boolean next() throws IOException {
			ensureInitialized(); //can't initialize before as it would not be allowed to throw IOException
			return iterator.next();
		}

		@Override
		public boolean skipTo(int target) throws IOException {
			ensureInitialized(); //can't initialize before as it would not be allowed to throw IOException
			return iterator.skipTo( target );
		}
		
		private final void ensureInitialized() throws IOException {
			if ( iterator == null ) {
				iterator = buildBitset().iterator();
			}
		}
		
	}
	
}
