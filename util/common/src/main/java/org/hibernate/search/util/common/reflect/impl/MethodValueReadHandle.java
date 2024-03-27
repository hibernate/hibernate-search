/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public final class MethodValueReadHandle<T> implements ValueReadHandle<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Method method;

	public MethodValueReadHandle(Method method) {
		this.method = method;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + method + "]";
	}

	@Override
	@SuppressWarnings("unchecked")
	public T get(Object thiz) {
		try {
			return (T) method.invoke( thiz );
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw log.errorInvokingMember( method, Throwables.safeToString( e, thiz ), e, e.getMessage() );
		}
		catch (InvocationTargetException e) {
			Throwable thrown = e.getCause();
			if ( thrown instanceof Error ) {
				throw (Error) thrown;
			}
			else {
				throw log.errorInvokingMember( method, Throwables.safeToString( thrown, thiz ), thrown, thrown.getMessage() );
			}
		}
	}

	@Override
	public int hashCode() {
		return method.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().equals( getClass() ) ) {
			return false;
		}
		MethodValueReadHandle<?> other = (MethodValueReadHandle<?>) obj;
		return method.equals( other.method );
	}

}
