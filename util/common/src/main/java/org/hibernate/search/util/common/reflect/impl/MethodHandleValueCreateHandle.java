/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Member;

import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.CommonMiscLog;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;

public final class MethodHandleValueCreateHandle<T> implements ValueCreateHandle<T> {

	private final Member member;
	private final MethodHandle delegate;

	public MethodHandleValueCreateHandle(Member member, MethodHandle delegate) {
		this.member = member;
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + member + "]";
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
		MethodHandleValueCreateHandle<?> other = (MethodHandleValueCreateHandle<?>) obj;
		return member.equals( other.member );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T create(Object... arguments) {
		try {
			return (T) delegate.invokeWithArguments( arguments );
		}
		catch (Error e) {
			throw e;
		}
		catch (Throwable e) {
			if ( e instanceof InterruptedException ) {
				Thread.currentThread().interrupt();
			}
			throw CommonMiscLog.INSTANCE.errorInvokingStaticMember( member, Throwables.safeToString( e, arguments ), e,
					e.getMessage() );
		}
	}

}
