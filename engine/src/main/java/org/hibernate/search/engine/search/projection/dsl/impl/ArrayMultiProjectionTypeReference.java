/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.lang.reflect.Array;
import java.util.List;

import org.hibernate.search.engine.search.projection.dsl.MultiProjectionTypeReference;

public record ArrayMultiProjectionTypeReference<V>(Class<V> elementType) implements MultiProjectionTypeReference<V[], V> {

	public static <V> MultiProjectionTypeReference<V[], V> instance(Class<V> componentType) {
		return new ArrayMultiProjectionTypeReference<>( componentType );
	}

	@SuppressWarnings("unchecked")
	@Override
	public V[] convert(List<V> list) {
		V[] array = (V[]) Array.newInstance( elementType, list.size() );
		int i = 0;
		for ( V v : list ) {
			array[i++] = v;
		}
		return array;
	}

}
