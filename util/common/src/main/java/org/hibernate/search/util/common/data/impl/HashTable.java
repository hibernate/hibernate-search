/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A hash table, i.e. a mapping between keys and values involving a {@link HashFunction}.
 *
 * @param <T> The type of elements stored in each bucket.
 */
public abstract class HashTable<T> implements Iterable<T> {

	final Object[] buckets;

	HashTable(int size) {
		this.buckets = new Object[size];
	}

	/**
	 * @return The size of this hash table, i.e. the number of buckets.
	 */
	public final int size() {
		return buckets.length;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int index = 0;

			@Override
			public boolean hasNext() {
				return index < buckets.length;
			}

			@Override
			@SuppressWarnings("unchecked")
			public T next() {
				if ( !hasNext() ) {
					throw new NoSuchElementException();
				}
				return (T) buckets[index++];
			}
		};
	}

	/**
	 * @param key A key to hash in order to compute an index.
	 * @return The content of the bucket assigned to the given {@code key}.
	 */
	public final T get(CharSequence key) {
		return get( computeIndex( key ) );
	}

	/**
	 * @param index The index of a bucket in this hash table.
	 * @return The content of the bucket at index {@code index}.
	 * @throws ArrayIndexOutOfBoundsException If the given index is negative or higher than the table's size.
	 */
	@SuppressWarnings("unchecked")
	public final T get(int index) {
		return (T) buckets[index];
	}

	/**
	 * @param index The index of a bucket in this hash table.
	 * @param value The value to set for the bucket at index {@code index}.
	 * @throws ArrayIndexOutOfBoundsException If the given index is negative or higher than the table's size.
	 */
	public final void set(int index, T value) {
		buckets[index] = value;
	}

	/**
	 * Hashes a {@code key} and computes an array index based on that hash.
	 * <p>
	 * The maximum index is defined by constructor parameters passed to the hash function.
	 *
	 * @param key A key to hash in order to compute an index.
	 * @return The index to use for the given {@code key} in a hash table of size {@code size}.
	 */
	public abstract int computeIndex(CharSequence key);
}
