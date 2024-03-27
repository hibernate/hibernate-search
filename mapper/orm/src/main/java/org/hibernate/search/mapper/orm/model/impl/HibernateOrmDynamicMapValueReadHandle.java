/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.Map;
import java.util.Objects;

import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

final class HibernateOrmDynamicMapValueReadHandle<T> implements ValueReadHandle<T> {

	private final String name;
	private final Class<T> type;

	HibernateOrmDynamicMapValueReadHandle(String name, Class<T> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		return getClass().getName() + "[" + type.getName() + " " + name + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().equals( getClass() ) ) {
			return false;
		}
		HibernateOrmDynamicMapValueReadHandle<?> other = (HibernateOrmDynamicMapValueReadHandle<?>) obj;
		return name.equals( other.name ) && type.equals( other.type );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, type );
	}

	@Override
	public T get(Object thiz) {
		return type.cast( ( (Map<?, ?>) thiz ).get( name ) );
	}
}
