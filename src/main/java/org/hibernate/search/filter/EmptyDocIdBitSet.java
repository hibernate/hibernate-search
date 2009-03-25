// $Id$
package org.hibernate.search.filter;

import java.io.Serializable;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * A DocIdSet which is always empty.
 * Stateless and ThreadSafe.
 * 
 * @author Sanne Grinovero
 */
public final class EmptyDocIdBitSet extends DocIdSet implements Serializable {

	private static final long serialVersionUID = 6429929383767238322L;

	public static final DocIdSet instance = new EmptyDocIdBitSet();
	
	private static final DocIdSetIterator iterator = new EmptyDocIdSetIterator();
	
	private EmptyDocIdBitSet(){
		// is singleton
	}

	@Override
	public final DocIdSetIterator iterator() {
		return iterator;
	}

	/**
	 * implements a DocIdSetIterator for an empty DocIdSet
	 * As it is empty it also is stateless and so it can be reused.
	 */
	private static final class EmptyDocIdSetIterator extends DocIdSetIterator {

		@Override
		public final int doc() {
			throw new IllegalStateException( "Should never be called" );
		}

		@Override
		public final boolean next() {
			return false;
		}

		@Override
		public final boolean skipTo(int target) {
			return false;
		}

	}
	
}
