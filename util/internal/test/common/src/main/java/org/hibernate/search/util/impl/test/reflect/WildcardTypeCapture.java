/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A type capture used when the captured type cannot be used as a type argument to an anonymous type,
 * e.g. the capture type is a wildcard type.
 */
public abstract class WildcardTypeCapture<T extends WildcardTypeCapture.Of<?>> extends TypeCapture<T> {
	@Override
	Type capture() {
		return captureTypeArgument( WildcardTypeCapture.class, this );
	}

	public static <T> Type captureTypeArgument(Class<T> superType, T instance) {
		Type ofType = TypeCapture.captureTypeArgument( superType, instance );
		if ( !( ofType instanceof ParameterizedType ) ) {
			throw new IllegalArgumentException( ofType + " isn't parameterized" );
		}
		ParameterizedType parameterizedOfType = (ParameterizedType) ofType;
		if ( !Of.class.equals( parameterizedOfType.getRawType() ) ) {
			throw new IllegalArgumentException( ofType + " isn't a parameterized version of " + Of.class );
		}
		return parameterizedOfType.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unused")
	public interface Of<T> {
	}
}
