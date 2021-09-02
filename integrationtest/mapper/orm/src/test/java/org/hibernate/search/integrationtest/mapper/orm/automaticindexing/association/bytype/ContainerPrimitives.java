/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public interface ContainerPrimitives<C, V> {

	void add(C container, V value);

	void remove(C container, V value);

	void clear(C container);

	static <C extends Collection<T>, T> ContainerPrimitives<C, T> collection() {
		return new ContainerPrimitives<C, T>() {
			@Override
			public void add(C container, T value) {
				container.add( value );
			}

			@Override
			public void remove(C container, T value) {
				container.remove( value );
			}

			@Override
			public void clear(C container) {
				container.clear();
			}
		};
	}

	static <M extends Map<K, String>, K> ContainerPrimitives<M, K> mapKeys(Function<K, String> keyToValue) {
		return new ContainerPrimitives<M, K>() {
			@Override
			public void add(M container, K key) {
				container.put( key, keyToValue.apply( key ) );
			}

			@Override
			public void remove(M container, K key) {
				container.remove( key );
			}

			@Override
			public void clear(M container) {
				container.clear();
			}
		};
	}

	static <M extends Map<String, V>, V> ContainerPrimitives<M, V> mapValues(Function<V, String> valueToKey) {
		return new ContainerPrimitives<M, V>() {
			@Override
			public void add(M container, V value) {
				container.put( valueToKey.apply( value ), value );
			}

			@Override
			public void remove(M container, V value) {
				container.remove( valueToKey.apply( value ) );
			}

			@Override
			public void clear(M container) {
				container.clear();
			}
		};
	}

}
