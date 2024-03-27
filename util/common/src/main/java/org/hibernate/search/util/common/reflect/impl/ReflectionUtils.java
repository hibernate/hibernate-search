/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Optional;

import org.hibernate.search.util.common.AssertionFailure;

public final class ReflectionUtils {

	private ReflectionUtils() {
	}

	public static Class<?> getRawType(Type type) {
		if ( type instanceof TypeVariable ) {
			Type[] upperBounds = ( (TypeVariable<?>) type ).getBounds();
			return getRawType( upperBounds[0] );
		}
		else if ( type instanceof WildcardType ) {
			Type[] upperBounds = ( (WildcardType) type ).getUpperBounds();
			return getRawType( upperBounds[0] );
		}
		else if ( type instanceof ParameterizedType ) {
			return getRawType( ( (ParameterizedType) type ).getRawType() );
		}
		else if ( type instanceof Class ) {
			return (Class<?>) type;
		}
		else if ( type instanceof GenericArrayType ) {
			Class<?> rawElementType = getRawType( ( (GenericArrayType) type ).getGenericComponentType() );
			return getArrayClass( rawElementType );
		}
		else {
			throw new AssertionFailure( "Unexpected java.lang.reflect.Type type: " + type.getClass() );
		}
	}

	public static Optional<Type> getArrayElementType(Type type) {
		if ( type instanceof TypeVariable ) {
			Type[] upperBounds = ( (TypeVariable<?>) type ).getBounds();
			return getArrayElementType( upperBounds[0] );
		}
		else if ( type instanceof WildcardType ) {
			Type[] upperBounds = ( (WildcardType) type ).getUpperBounds();
			return getArrayElementType( upperBounds[0] );
		}
		else if ( type instanceof ParameterizedType ) {
			return Optional.empty();
		}
		else if ( type instanceof Class ) {
			Class<?> clazz = (Class<?>) type;
			return Optional.ofNullable( clazz.getComponentType() );
		}
		else if ( type instanceof GenericArrayType ) {
			return Optional.of( ( (GenericArrayType) type ).getGenericComponentType() );
		}
		else {
			throw new AssertionFailure( "Unexpected java.lang.reflect.Type type: " + type.getClass() );
		}
	}

	private static Class<?> getArrayClass(Class<?> rawElementType) {
		// This is ugly, but apparently the only way to get an array type from an element type
		return Array.newInstance( rawElementType, 0 ).getClass();
	}

}
