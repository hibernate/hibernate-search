package org.hibernate.search.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

public class EmptyDocIdBitSet extends DocIdSet {

	public static final DocIdSet instance = new EmptyDocIdBitSet();
	
	private final DocIdSetIterator iterator = new EmptyDocIdSetIterator();
	
	private EmptyDocIdBitSet(){
		// is singleton
	}

	@Override
	public DocIdSetIterator iterator() {
		return iterator;
	}

	/**
	 * implements a DocIdSetIterator for an empty DocIdSet
	 * As it is empty it also is stateless and so it can be reused.
	 */
	private static class EmptyDocIdSetIterator extends DocIdSetIterator {

		@Override
		public int doc() {
			throw new IllegalStateException("Should never be called");
		}

		@Override
		public boolean next() throws IOException {
			return false;
		}

		@Override
		public boolean skipTo(int target) throws IOException {
			return false;
		}

	}
	
}
