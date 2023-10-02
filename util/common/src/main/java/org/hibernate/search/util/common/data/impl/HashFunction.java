/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.impl;

/**
 * A hash function, for use in hash tables.
 */
public interface HashFunction {

	/**
	 * Hashes a {@code key}, i.e. turns it into an integer for use in a {@link HashTable}.
	 * @param key A key to hash.
	 * @return A hash.
	 */
	int hash(CharSequence key);

}
