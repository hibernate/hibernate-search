/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.impl;

import java.util.Arrays;

import org.hibernate.search.util.common.data.Range;

/**
 * A hash table that derives an index from hashes using
 * a partition of the hashing space based on contiguous ranges.
 * <p>
 * This makes this table particularly suitable in situations where the hash must be computed
 * before the number of buckets is known, then stored, then queried efficiently
 * (e.g. retrieve all entries for a given bucket by querying
 * {@code WHERE hash BETWEEN bucketLowerBound AND bucketUpperBound}).
 * This wouldn't be as efficient with a modulo-based hash table such as {@link ModuloHashTable},
 * since the modulo operation would have to be applied to all entries at query time
 * (it cannot be indexed since the number of buckets is not known when hashing).
 *
 * @param <T> The type of elements stored in each bucket.
 */
public final class RangeHashTable<T> extends HashTable<T> {

	private final RangeCompatibleHashFunction hashFunction;
	private final Range<Integer>[] bucketRanges;
	private final int[] lowerBounds;

	@SuppressWarnings("unchecked") // We aren't allowed to create generic arrays, so we have to use a raw type here.
	public RangeHashTable(RangeCompatibleHashFunction hashFunction, int size) {
		super( size );
		this.hashFunction = hashFunction;

		// Split the integer space into <size> ranges of equal width.
		bucketRanges = new Range[size];
		if ( size == 1 ) {
			bucketRanges[0] = Range.all();
		}
		else {
			long integerSpaceWidth = ( (long) Integer.MAX_VALUE ) - Integer.MIN_VALUE + 1;
			int rangeWidth = (int) ( integerSpaceWidth / size );
			int upperBound = Integer.MIN_VALUE + rangeWidth;
			bucketRanges[0] = Range.lessThan( Integer.MIN_VALUE + rangeWidth );
			for ( int i = 1; i < ( size - 1 ); i++ ) {
				int lowerBound = upperBound;
				upperBound = lowerBound + rangeWidth;
				bucketRanges[i] = Range.canonical( lowerBound, upperBound );
			}
			int lowerBound = upperBound;
			bucketRanges[size - 1] = Range.atLeast( lowerBound );
		}

		// Populate lower bounds for use in computeIndexForHash
		lowerBounds = new int[size];
		for ( int i = 0; i < size; i++ ) {
			lowerBounds[i] = bucketRanges[i].lowerBoundValue().orElse( Integer.MIN_VALUE );
		}
	}

	@Override
	public int computeIndex(CharSequence key) {
		return computeIndexForHash( hashFunction.hash( key ) );
	}

	// Exposed for testing only
	public int computeIndexForHash(int hash) {
		int searchResult = Arrays.binarySearch( lowerBounds, hash );
		return searchResult >= 0 ? searchResult : -( searchResult + 2 );
	}

	/**
	 * @param index An index between {@code 0} and {@link #size()} (exclusive).
	 *
	 * @return The range of {@link HashFunction#hash(CharSequence) hashes} associated to the bucket for that index.
	 */
	public Range<Integer> rangeForBucket(int index) {
		return bucketRanges[index];
	}

}
