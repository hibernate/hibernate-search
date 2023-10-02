/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.impl;

/**
 * A hash table that applies a modulo operation to the hash in order to turn it into an index.
 *
 * @param <T> The type of elements stored in each bucket.
 */
public final class ModuloHashTable<T> extends HashTable<T> {
	final HashFunction hashFunction;

	public ModuloHashTable(HashFunction hashFunction, int size) {
		super( size );
		this.hashFunction = hashFunction;
	}

	@Override
	public int computeIndex(CharSequence key) {
		// WARNING: NEVER CHANGE THIS IMPLEMENTATION
		// This is used to persist data (picking a shard in a Lucene index in particular)
		return Math.abs( hashFunction.hash( key ) % buckets.length );
	}
}
