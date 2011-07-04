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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.DocIdBitSet;
import org.apache.lucene.util.OpenBitSet;

/**
 * Helper class to apply some common optimizations when
 * several Filters are applied.
 * 
 * @author Sanne Grinovero
 */
public class FilterOptimizationHelper {

	/**
	 * Returns a new list of DocIdSet, applying binary AND
	 * on all DocIdSet implemented by using BitSet or OpenBitSet.
	 * @param docIdSets
	 * @return the same list if no changes were done
	 */
	public static List<DocIdSet> mergeByBitAnds(List<DocIdSet> docIdSets) {
		int size = docIdSets.size();
		List<OpenBitSet> openBitSets = new ArrayList<OpenBitSet>( size );
		List<DocIdBitSet> docIdBitSets = new ArrayList<DocIdBitSet>( size );
		List<DocIdSet> nonMergeAble = new ArrayList<DocIdSet>( size );
		for (DocIdSet set : docIdSets) {
			if (set instanceof OpenBitSet) {
				openBitSets.add( (OpenBitSet) set );
			}
			else if (set instanceof DocIdBitSet) {
				docIdBitSets.add( (DocIdBitSet) set );
			}
			else {
				nonMergeAble.add( set );
			}
		}
		if ( openBitSets.size() <= 1 && docIdBitSets.size() <= 1 ) {
			//skip all work as no optimization is possible
			return docIdSets;
		}
		if ( openBitSets.size() > 0 ) {
			nonMergeAble.add( mergeByBitAndsForOpenBitSet( openBitSets ) );
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
		for ( int i=1; i<listSize; i++ ) {
			BitSet bitSet = docIdBitSets.get( i ).getBitSet();
			result.and( bitSet );
		}
		return new DocIdBitSet( result );
	}

	/**
	 * Merges all OpenBitSet in a new OpenBitSet using
	 * binary AND operations, which is usually more efficient
	 * than using an iterator.
	 * @param openBitSets
	 * @return a new OpenBitSet, or the first element if only
	 * one element was found in the list.
	 */
	private static OpenBitSet mergeByBitAndsForOpenBitSet(List<OpenBitSet> openBitSets) {
		int listSize = openBitSets.size();
		if ( listSize == 1 ) {
			return openBitSets.get( 0 );
		}
		//we need to copy the first OpenBitSet because BitSet is modified by .logicalOp
		OpenBitSet result = (OpenBitSet) openBitSets.get( 0 ).clone();
		for ( int i=1; i<listSize; i++ ) {
			OpenBitSet openSet = openBitSets.get( i );
			result.intersect( openSet );
		}
		return result;
	}

}
