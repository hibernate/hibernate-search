/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.search.util.common.reflect.impl.ConstructorValueCreateHandle;
import org.hibernate.search.util.common.reflect.impl.FieldValueReadHandle;
import org.hibernate.search.util.common.reflect.impl.MethodValueReadHandle;

@SuppressWarnings("deprecation")
final class MemberValueHandleFactory implements ValueHandleFactory, ValueReadHandleFactory {
	@Override
	public <T> ValueCreateHandle<T> createForConstructor(Constructor<T> constructor) {
		return new ConstructorValueCreateHandle<>( constructor );
	}

	@Override
	public ValueReadHandle<?> createForField(Field field) {
		return new FieldValueReadHandle<>( field );
	}

	@Override
	public ValueReadHandle<?> createForMethod(Method method) {
		return new MethodValueReadHandle<>( method );
	}
}
