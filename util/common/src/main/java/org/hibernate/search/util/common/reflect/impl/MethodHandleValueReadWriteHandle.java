/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Member;

import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.CommonMiscLog;
import org.hibernate.search.util.common.reflect.spi.ValueReadWriteHandle;

public final class MethodHandleValueReadWriteHandle<T> implements ValueReadWriteHandle<T> {

	private final Member member;
	private final MethodHandle setter;
	private final MethodHandle getter;

	public MethodHandleValueReadWriteHandle(Member member, MethodHandle setter, MethodHandle getter) {
		this.member = member;
		this.setter = setter;
		this.getter = getter;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + member + "]";
	}

	@Override
	@SuppressWarnings("unchecked")
	public T get(Object thiz) {
		try {
			return (T) getter.invoke( thiz );
		}
		catch (Error e) {
			throw e;
		}
		catch (Throwable e) {
			if ( e instanceof InterruptedException ) {
				Thread.currentThread().interrupt();
			}
			throw CommonMiscLog.INSTANCE.errorInvokingMember( member, Throwables.safeToString( e, thiz ), e,
					e.getMessage() );
		}
	}

	@Override
	public void set(Object thiz, Object value) {
		try {
			setter.invoke( thiz, value );
		}
		catch (Error e) {
			throw e;
		}
		catch (Throwable e) {
			if ( e instanceof InterruptedException ) {
				Thread.currentThread().interrupt();
			}
			throw CommonMiscLog.INSTANCE.errorInvokingMember( member, Throwables.safeToString( e, thiz ), e,
					e.getMessage() );
		}
	}

	@Override
	public int hashCode() {
		return member.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().equals( getClass() ) ) {
			return false;
		}
		MethodHandleValueReadWriteHandle<?> other = (MethodHandleValueReadWriteHandle<?>) obj;
		return member.equals( other.member );
	}

}
