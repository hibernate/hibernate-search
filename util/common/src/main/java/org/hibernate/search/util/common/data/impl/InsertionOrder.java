/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A helper to generate map keys that can be sorted according to their insertion order.
 * <p>
 * Useful with {@link java.util.concurrent.ConcurrentSkipListMap} in particular,
 * to get insertion order instead of natural key order.
 * <p>
 * Do not use with maps whose keys can be removed: this class cannot forget about keys,
 * so it can lead to memory leaks in that case.
 */
public final class InsertionOrder<T> {

	private final AtomicInteger indexProvider = new AtomicInteger();
	private final Map<T, Key<T>> keys = new ConcurrentHashMap<>();
	private final Function<? super T, Key<T>> createNewKey =
			key -> new Key<>( indexProvider.getAndIncrement(), key );

	public Key<T> wrapKey(T key) {
		return keys.computeIfAbsent( key, createNewKey );
	}

	public static final class Key<T> implements Comparable<Key<?>> {
		private final int index;
		private final T wrapped;

		private Key(int index, T wrapped) {
			this.index = index;
			this.wrapped = wrapped;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Key<?> key = (Key<?>) o;
			return index == key.index;
		}

		@Override
		public int hashCode() {
			return Objects.hash( index );
		}

		@Override
		public int compareTo(Key<?> o) {
			return Integer.compare( index, o.index );
		}

		public T get() {
			return wrapped;
		}
	}

}
