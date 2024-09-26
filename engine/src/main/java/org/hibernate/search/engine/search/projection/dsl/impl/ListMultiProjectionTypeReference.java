/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.projection.dsl.MultiProjectionTypeReference;

public class ListMultiProjectionTypeReference<V> implements MultiProjectionTypeReference<List<V>, V> {

	@SuppressWarnings("rawtypes")
	private static final ListMultiProjectionTypeReference INSTANCE = new ListMultiProjectionTypeReference();

	@SuppressWarnings("unchecked")
	public static <V> MultiProjectionTypeReference<List<V>, V> instance() {
		return INSTANCE;
	}

	@Override
	public List<V> convert(List<V> list) {
		return list;
	}

	@Override
	public List<V> empty() {
		return List.of();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
