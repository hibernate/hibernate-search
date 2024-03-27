/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public interface ContainerPrimitives<C, V> {

	void add(C container, V value);

	void remove(C container, V value);

	void clear(C container);

	Iterator<V> iterator(C container);

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

			@Override
			public Iterator<T> iterator(C container) {
				return container.iterator();
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

			@Override
			public Iterator<K> iterator(M container) {
				return container.keySet().iterator();
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

			@Override
			public Iterator<V> iterator(M container) {
				return container.values().iterator();
			}
		};
	}

}
