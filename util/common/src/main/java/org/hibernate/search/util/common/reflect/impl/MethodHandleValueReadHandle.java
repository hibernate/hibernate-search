/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Member;

import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public final class MethodHandleValueReadHandle<T> implements ValueReadHandle<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Member member;
	private final MethodHandle getter;

	public MethodHandleValueReadHandle(Member member, MethodHandle getter) {
		this.member = member;
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
			throw log.errorInvokingMember( member, Throwables.safeToString( e, thiz ), e, e.getMessage() );
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
		MethodHandleValueReadHandle<?> other = (MethodHandleValueReadHandle<?>) obj;
		return member.equals( other.member );
	}

}
