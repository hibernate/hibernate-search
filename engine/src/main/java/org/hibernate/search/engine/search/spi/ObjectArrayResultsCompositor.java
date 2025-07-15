/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.spi;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
abstract class ObjectArrayResultsCompositor<V>
		implements ResultsCompositor<Object[], V> {

	private final int size;

	ObjectArrayResultsCompositor(int size) {
		this.size = size;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + transformer() + "]";
	}

	protected abstract Object transformer();

	@Override
	public Object[] createInitial() {
		return new Object[size];
	}

	@Override
	public Object[] set(Object[] components, int index, Object value) {
		components[index] = value;
		return components;
	}

	@Override
	public Object get(Object[] components, int index) {
		return components[index];
	}

	@Override
	public abstract V finish(Object[] components);

}
