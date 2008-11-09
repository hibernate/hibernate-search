package org.hibernate.search.filter;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import static java.lang.Math.max;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.DocIdBitSet;

/**
 * A DocIdSet built as applying "AND" operation to a list of other DocIdSet.
 * The DocIdSetIterator returned will return only document ids contained
 * in all DocIdSet handed to the constructor.
 * 
 * @author Sanne Grinovero
 */
public class AndDocIdSet extends DocIdSet {
	
	private DocIdBitSet docIdBitSet;
	private final List<DocIdSet> andedDocIdSets;
	
	public AndDocIdSet(List<DocIdSet> andedDocIdSets) {
		if ( andedDocIdSets == null || andedDocIdSets.size() < 2 )
			throw new IllegalArgumentException( "To \"and\" some DocIdSet they should be at least 2" );
		this.andedDocIdSets = andedDocIdSets;
	}
	
	private synchronized void buildBitset() throws IOException {
		if ( docIdBitSet != null ) return; // double check for concurrent initialization
		//TODO if all andedDocIdSets are actually DocIdBitSet, use their internal BitSet instead of next algo.
		//TODO if some andedDocIdSets are DocIdBitSet, merge them first.
		int size = andedDocIdSets.size();
		DocIdSetIterator[] iterators = new DocIdSetIterator[size];
		int[] positions = new int[size];
		boolean valuesExist = true;
		int maxIndex = 0;
		for (int i=0; i<size; i++) {
			// build all iterators
			DocIdSetIterator iterator = andedDocIdSets.get(i).iterator();
			iterators[i] = iterator;
			// and move to first position
			boolean nextExists = iterator.next();
			if ( ! nextExists ) {
				valuesExist = false;
				break;
			}
			int currentFilterValue = iterator.doc();
			positions[i] = currentFilterValue;
			// find the initial maximum position
			maxIndex = max( maxIndex, currentFilterValue );
		}
		BitSet bitSet = new BitSet();
		if ( valuesExist ) { // skip further processing if some idSet is empty
			do {
				if ( allSame( positions ) ) {
					// enable a bit if all idSets agree on it:
					bitSet.set( maxIndex );
					maxIndex++;
				}
				maxIndex = advance( iterators, positions, maxIndex );
			} while ( maxIndex != -1 ); // -1 means the end of some bitSet has been reached (end condition)
		}
		docIdBitSet = new DocIdBitSet( bitSet );
	}

	/**
	 * Have all DocIdSetIterator having current doc id minor than currentMaxPosition
	 * skip to at least this position.
	 * @param iterators
	 * @param positions
	 * @return maximum position of all DocIdSetIterator after the operation, or -1 when at least one reached the end.
	 * @throws IOException 
	 */
	private final int advance(final DocIdSetIterator[] iterators, final int[] positions, int currentMaxPosition) throws IOException {
		for (int i=0; i<positions.length; i++) {
			if ( positions[i] != currentMaxPosition ) {
				boolean validPosition = iterators[i].skipTo( currentMaxPosition );
				if ( ! validPosition )
					return -1;
				positions[i] = iterators[i].doc();
				currentMaxPosition = max( currentMaxPosition, positions[i] );
			}
		}
		return currentMaxPosition;
	}

	/**
	 * see if all DocIdSetIterator stopped at the same position.
	 * @param positions the array of current positions.
	 * @return true if all DocIdSetIterator agree on the current docId.
	 */
	private final boolean allSame(final int[] positions) {
		int base = positions[0];
		for (int i=1; i<positions.length; i++) {
			if ( base != positions[i] )
				return false;
		}
		return true;
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
		
		private void ensureInitialized() throws IOException {
			if ( docIdBitSet == null ) buildBitset();
			if ( iterator == null ) {
				iterator = docIdBitSet.iterator();
			}
		}
		
	}
	
}
