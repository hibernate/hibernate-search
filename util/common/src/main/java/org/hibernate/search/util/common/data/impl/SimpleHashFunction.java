/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.impl;

/**
 * A fast, but cryptographically insecure hash function,
 * based on Java's {@link String#toString()}.
 */
public final class SimpleHashFunction implements HashFunction {

	public static final SimpleHashFunction INSTANCE = new SimpleHashFunction();

	private SimpleHashFunction() {
	}

	@Override
	public String toString() {
		return "SimpleHashFunction";
	}

	/**
	 * Hashes a {@code key}, i.e. turns it into an integer for use in a {@link HashTable}.
	 * <p>
	 * This implementation is the same hash function as Java's String.toString().
	 * <p>
	 * It does not delegate to String.toString() in order to protect against
	 * future changes in the JDK (?) or different JDK implementations,
	 * so that the resulting hash can safely be used for persistence
	 * (e.g. to route data to a file).
	 *
	 * @param key A key to hash.
	 * @return A hash.
	 */
	@Override
	public int hash(CharSequence key) {
		// WARNING: NEVER CHANGE THIS IMPLEMENTATION
		// This is used to persist data (picking a shard in a Lucene index in particular)
		int hash = 0;
		int length = key.length();
		for ( int index = 0; index < length; index++ ) {
			hash = 31 * hash + key.charAt( index );
		}
		return hash;
	}

}
