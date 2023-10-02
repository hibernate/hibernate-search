/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.reflect.impl.MethodHandleValueCreateHandle;
import org.hibernate.search.util.common.reflect.impl.MethodHandleValueReadHandle;

@SuppressWarnings("deprecation")
@SuppressForbiddenApis(reason = "MethodHandles don't always work, but usage of this class is configurable,"
		+ " so it should only be used in contexts where MethodHandles actually work.")
final class MethodHandleValueHandleFactory implements ValueHandleFactory, ValueReadHandleFactory {

	private final MethodHandles.Lookup lookup;

	MethodHandleValueHandleFactory(MethodHandles.Lookup lookup) {
		this.lookup = lookup;
	}

	@Override
	public <T> ValueCreateHandle<T> createForConstructor(Constructor<T> constructor) throws IllegalAccessException {
		return new MethodHandleValueCreateHandle<>( constructor, lookup.unreflectConstructor( constructor ) );
	}

	@Override
	public ValueReadHandle<?> createForField(Field field) throws IllegalAccessException {
		return new MethodHandleValueReadHandle<>( field, lookup.unreflectGetter( field ) );
	}

	@Override
	public ValueReadHandle<?> createForMethod(Method method) throws IllegalAccessException {
		return new MethodHandleValueReadHandle<>( method, lookup.unreflect( method ) );
	}
}
