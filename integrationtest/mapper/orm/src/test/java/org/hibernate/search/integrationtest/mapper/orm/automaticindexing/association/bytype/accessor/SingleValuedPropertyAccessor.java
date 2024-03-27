/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor;

import java.util.function.BiConsumer;
import java.util.function.Function;

final class SingleValuedPropertyAccessor<R, V> implements PropertyAccessor<R, V> {

	private final BiConsumer<R, V> setMethod;
	private final Function<R, V> getMethod;

	SingleValuedPropertyAccessor(BiConsumer<R, V> setMethod) {
		this( setMethod, null );
	}

	SingleValuedPropertyAccessor(BiConsumer<R, V> setMethod, Function<R, V> getMethod) {
		this.setMethod = setMethod;
		this.getMethod = getMethod;
	}

	@Override
	public String toString() {
		return getMethod != null ? getMethod.toString() : setMethod.toString();
	}

	@Override
	public void set(R root, V value) {
		setMethod.accept( root, value );
	}

	@Override
	public V get(R root) {
		if ( getMethod == null ) {
			throw new UnsupportedOperationException();
		}
		return getMethod.apply( root );
	}

	@Override
	public void clear(R root) {
		set( root, null );
	}

}
