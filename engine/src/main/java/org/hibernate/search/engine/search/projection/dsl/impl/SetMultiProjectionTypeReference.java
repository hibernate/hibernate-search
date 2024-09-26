/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.search.projection.dsl.MultiProjectionTypeReference;

public class SetMultiProjectionTypeReference<V> implements MultiProjectionTypeReference<Set<V>, V> {

	@SuppressWarnings("rawtypes")
	private static final SetMultiProjectionTypeReference INSTANCE = new SetMultiProjectionTypeReference();

	@SuppressWarnings("unchecked")
	public static <V> MultiProjectionTypeReference<Set<V>, V> instance() {
		return INSTANCE;
	}

	@Override
	public Set<V> convert(List<V> list) {
		return new HashSet<>( list );
	}

	@Override
	public Set<V> empty() {
		return Set.of();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
