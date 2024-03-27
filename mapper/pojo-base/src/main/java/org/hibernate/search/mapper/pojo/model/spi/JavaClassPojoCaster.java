/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

public final class JavaClassPojoCaster<T> implements PojoCaster<T> {
	private final Class<T> clazz;

	public JavaClassPojoCaster(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + clazz.getSimpleName() + "]";
	}

	@Override
	public T cast(Object object) {
		return clazz.cast( object );
	}

	@Override
	public T castOrNull(Object object) {
		if ( clazz.isInstance( object ) ) {
			return clazz.cast( object );
		}
		else {
			return null;
		}
	}
}
