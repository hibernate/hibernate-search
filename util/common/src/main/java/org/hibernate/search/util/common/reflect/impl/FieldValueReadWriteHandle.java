/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.reflect.Field;

import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.CommonMiscLog;
import org.hibernate.search.util.common.reflect.spi.ValueReadWriteHandle;

public final class FieldValueReadWriteHandle<T> implements ValueReadWriteHandle<T> {

	private final Field field;

	public FieldValueReadWriteHandle(Field field) {
		this.field = field;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + field + "]";
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(Object thiz) {
		try {
			return (T) field.get( thiz );
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw CommonMiscLog.INSTANCE.errorInvokingMember( field, Throwables.safeToString( e, thiz ), e,
					e.getMessage() );
		}
	}

	@Override
	public void set(Object thiz, Object value) {
		try {
			field.set( thiz, value );
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw CommonMiscLog.INSTANCE.errorInvokingMember( field, Throwables.safeToString( e, thiz ), e,
					e.getMessage() );
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
		FieldValueReadWriteHandle<?> other = (FieldValueReadWriteHandle<?>) obj;
		return field.equals( other.field );
	}

}
