/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.search.engine.search.projection.dsl.MultiProjectionTypeReference;

public class SortedSetMultiProjectionTypeReference<V> implements MultiProjectionTypeReference<SortedSet<V>, V> {

	@SuppressWarnings("rawtypes")
	private static final SortedSetMultiProjectionTypeReference INSTANCE = new SortedSetMultiProjectionTypeReference();

	@SuppressWarnings("unchecked")
	public static <V> MultiProjectionTypeReference<SortedSet<V>, V> instance() {
		return INSTANCE;
	}

	public static <V> MultiProjectionTypeReference<SortedSet<V>, V> instance(Comparator<V> comparator) {
		return new ComparatorBasedSortedSetMultiProjectionTypeReference<>( comparator );
	}

	@Override
	public SortedSet<V> convert(List<V> list) {
		return new TreeSet<>( list );
	}

	private record ComparatorBasedSortedSetMultiProjectionTypeReference<V>(Comparator<V> comparator)
			implements MultiProjectionTypeReference<SortedSet<V>, V> {
		@Override
		public SortedSet<V> convert(List<V> list) {
			TreeSet<V> set = new TreeSet<>( comparator );
			set.addAll( list );
			return set;
		}
	}
}
