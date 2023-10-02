/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.function.Function;

final class SingleValuedProjectionCompositor<P1, V>
		implements ProjectionCompositor<Object, V> {
	private final Function<P1, V> transformer;

	SingleValuedProjectionCompositor(Function<P1, V> transformer) {
		this.transformer = transformer;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + transformer + "]";
	}


	@Override
	public P1 createInitial() {
		return null;
	}

	@Override
	public Object set(Object components, int index, Object value) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( "Invalid index passed to " + this + ": " + index );
		}
		return value;
	}

	@Override
	public Object get(Object components, int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( "Invalid index passed to " + this + ": " + index );
		}
		return components;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V finish(Object components) {
		return transformer.apply( (P1) components );
	}

}
