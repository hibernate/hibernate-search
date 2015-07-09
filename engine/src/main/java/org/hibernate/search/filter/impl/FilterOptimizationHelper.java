/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.DocIdBitSet;

/**
 * Helper class to apply some common optimizations when
 * several Filters are applied.
 *
 * @author Sanne Grinovero
 */
public final class FilterOptimizationHelper {

	private FilterOptimizationHelper() {
		//not allowed
	}

	/**
	 * Returns a new list of DocIdSet, applying binary AND
	 * on all DocIdSet implemented by using BitSet or OpenBitSet.
	 *
	 * @param docIdSets a {@link java.util.List} object.
	 * @return the same list if no changes were done
	 */
	public static List<DocIdSet> mergeByBitAnds(List<DocIdSet> docIdSets) {
		int size = docIdSets.size();
		List<DocIdBitSet> docIdBitSets = new ArrayList<DocIdBitSet>( size );
		List<DocIdSet> nonMergeAble = new ArrayList<DocIdSet>( size );
		for ( DocIdSet set : docIdSets ) {
			if ( set instanceof DocIdBitSet ) {
				docIdBitSets.add( (DocIdBitSet) set );
			}
			else {
				nonMergeAble.add( set );
			}
		}
		if ( docIdBitSets.size() <= 1 ) {
			//skip all work as no optimization is possible
			return docIdSets;
		}
		if ( docIdBitSets.size() > 0 ) {
			nonMergeAble.add( mergeByBitAndsForDocIdBitSet( docIdBitSets ) );
		}
		return nonMergeAble;
	}

	/**
	 * Merges all DocIdBitSet in a new DocIdBitSet using
	 * binary AND operations, which is usually more efficient
	 * than using an iterator.
	 * @param docIdBitSets
	 * @return a new DocIdBitSet, or the first element if only
	 * one element was found in the list.
	 */
	private static DocIdBitSet mergeByBitAndsForDocIdBitSet(List<DocIdBitSet> docIdBitSets) {
		int listSize = docIdBitSets.size();
		if ( listSize == 1 ) {
			return docIdBitSets.get( 0 );
		}
		//we need to copy the first BitSet because BitSet is modified by .logicalOp
		BitSet result = (BitSet) docIdBitSets.get( 0 ).getBitSet().clone();
		for ( int i = 1; i < listSize; i++ ) {
			BitSet bitSet = docIdBitSets.get( i ).getBitSet();
			result.and( bitSet );
		}
		return new DocIdBitSet( result );
	}
}
