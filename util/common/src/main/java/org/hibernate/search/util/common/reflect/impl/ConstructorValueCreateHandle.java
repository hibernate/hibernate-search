/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.CommonFailuresLog;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;

public final class ConstructorValueCreateHandle<T>
		implements ValueCreateHandle<T> {

	private final Constructor<T> constructor;

	public ConstructorValueCreateHandle(Constructor<T> constructor) {
		this.constructor = constructor;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + constructor + "]";
	}

	@Override
	public int hashCode() {
		return constructor.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().equals( getClass() ) ) {
			return false;
		}
		ConstructorValueCreateHandle<?> other = (ConstructorValueCreateHandle<?>) obj;
		return constructor.equals( other.constructor );
	}

	@Override
	public T create(Object... arguments) {
		try {
			return (T) constructor.newInstance( arguments );
		}
		catch (RuntimeException | IllegalAccessException | InstantiationException e) {
			throw CommonFailuresLog.INSTANCE.errorInvokingStaticMember( constructor, Throwables.safeToString( e, arguments ),
					e, e.getMessage() );
		}
		catch (InvocationTargetException e) {
			Throwable thrown = e.getCause();
			if ( thrown instanceof Error ) {
				throw (Error) thrown;
			}
			else {
				throw CommonFailuresLog.INSTANCE.errorInvokingStaticMember( constructor,
						Throwables.safeToString( thrown, arguments ),
						thrown, thrown.getMessage() );
			}
		}
	}
}
