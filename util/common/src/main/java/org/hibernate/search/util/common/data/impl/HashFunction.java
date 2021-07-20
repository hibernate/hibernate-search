/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
