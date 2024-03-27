/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public final class FieldValueReadHandle<T> implements ValueReadHandle<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Field field;

	public FieldValueReadHandle(Field field) {
		this.field = field;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + field + "]";
	}

	@Override
	@SuppressWarnings("unchecked")
	public T get(Object thiz) {
		try {
			return (T) field.get( thiz );
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw log.errorInvokingMember( field, Throwables.safeToString( e, thiz ), e, e.getMessage() );
		}
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().equals( getClass() ) ) {
			return false;
		}
		FieldValueReadHandle<?> other = (FieldValueReadHandle<?>) obj;
		return field.equals( other.field );
	}

}
