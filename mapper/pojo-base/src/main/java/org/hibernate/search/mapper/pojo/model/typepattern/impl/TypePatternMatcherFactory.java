/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.AssertionFailure;

public class TypePatternMatcherFactory {

	private final PojoBootstrapIntrospector introspector;

	/**
	 * @param introspector An introspector to use for reflection,
	 * mainly for {@link PojoBootstrapIntrospector#typeModel(Class)}.
	 */
	public TypePatternMatcherFactory(PojoBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	public TypePatternMatcher createExactRawTypeMatcher(Class<?> exactTypeToMatch) {
		PojoRawTypeModel<?> exactTypeToMatchModel = introspector.typeModel( exactTypeToMatch );
		return new ExactRawTypeMatcher( exactTypeToMatchModel );
	}

	public TypePatternMatcher createRawSuperTypeMatcher(Class<?> superTypeToMatch) {
		PojoRawTypeModel<?> superTypeToMatchModel = introspector.typeModel( superTypeToMatch );
		return new RawSuperTypeMatcher( superTypeToMatchModel );
	}

	/**
	 * @param typePattern The type used as a pattern to be matched. Not all types are accepted.
	 * @param typeToExtract The type to extract when matching the pattern. Not all types are accepted.
	 * @return A type pattern matcher matching subtypes of {@code typePattern}
	 * and returning the {@code typeToExtract} resolved against the type submitted to the matcher
	 * in the even of a match.
	 * @throws UnsupportedOperationException If this factory does not support creating a type pattern matcher
	 * for the given types.
	 */
	public ExtractingTypePatternMatcher createExtractingMatcher(Type typePattern, Type typeToExtract) {
		if ( typePattern instanceof TypeVariable ) {
			throw new UnsupportedOperationException( "Matching a type variable is not supported" );
		}
		else if ( typePattern instanceof WildcardType ) {
			throw new UnsupportedOperationException( "Matching a wildcard type is not supported" );
		}
		else if ( typePattern instanceof ParameterizedType ) {
			ParameterizedType parameterizedTypePattern = (ParameterizedType) typePattern;
			return createExtractingParameterizedTypeMatcher( parameterizedTypePattern, typeToExtract );
		}
		else if ( typePattern instanceof Class ) {
			Class<?> classTypePattern = (Class<?>) typePattern;
			return createExtractingClassTypeMatcher( classTypePattern, typeToExtract );
		}
		else if ( typePattern instanceof GenericArrayType ) {
			GenericArrayType arrayTypePattern = (GenericArrayType) typePattern;
			return createExtractingGenericArrayTypeMatcher( arrayTypePattern, typeToExtract );
		}
		else {
			throw new AssertionFailure( "Unexpected java.lang.reflect.Type type: " + typePattern.getClass() );
		}
	}

	private ExtractingTypePatternMatcher createExtractingGenericArrayTypeMatcher(GenericArrayType typePattern,
			Type typeToExtract) {
		if ( !( typeToExtract instanceof TypeVariable )
				|| !typePattern.getGenericComponentType().equals( typeToExtract ) ) {
			throw new UnsupportedOperationException(
					"Extracting anything other than the array element type when matching array types"
							+ " is not supported"
			);
		}
		TypeVariable<?> resultTypeVariable = (TypeVariable<?>) typeToExtract;
		Type[] upperBounds = resultTypeVariable.getBounds();
		if ( upperBounds.length > 1 || !Object.class.equals( upperBounds[0] ) ) {
			throw new UnsupportedOperationException(
					"Matching types with bounded type variables is not supported"
			);
		}
		return new ArrayElementTypeMatcher();
	}

	private ExtractingTypePatternMatcher createExtractingClassTypeMatcher(Class<?> typePattern,
			Type typeToExtract) {
		if ( !( typeToExtract instanceof Class ) ) {
			throw new UnsupportedOperationException(
					"Extracting a non-raw result type when matching a raw type is not supported"
			);
		}
		PojoRawTypeModel<?> typePatternModel = introspector.typeModel( typePattern );
		PojoRawTypeModel<?> typeToExtractModel = introspector.typeModel( (Class<?>) typeToExtract );
		return new ConstantExtractingTypePatternMatcherAdapter(
				new RawSuperTypeMatcher( typePatternModel ),
				typeToExtractModel
		);
	}

	private ExtractingTypePatternMatcher createExtractingParameterizedTypeMatcher(ParameterizedType typePattern,
			Type typeToExtract) {
		Class<?> rawTypePattern = (Class<?>) typePattern.getRawType();
		Type[] typeArguments = typePattern.getActualTypeArguments();

		Integer typeVariableIndex = null;
		for ( int i = 0; i < typeArguments.length; i++ ) {
			Type typeArgument = typeArguments[i];
			if ( typeArgument instanceof TypeVariable ) {
				if ( typeVariableIndex == null ) {
					typeVariableIndex = i;
					TypeVariable<?> typeVariable = (TypeVariable<?>) typeArgument;
					Type[] upperBounds = typeVariable.getBounds();
					if ( !typeToExtract.equals( typeVariable ) ) {
						throw new UnsupportedOperationException(
								"Extracting anything other than the type variable when matching parameterized types"
										+ " is not supported"
						);
					}
					if ( upperBounds.length > 1 || !Object.class.equals( upperBounds[0] ) ) {
						throw new UnsupportedOperationException(
								"Matching a parameterized type with bounded type arguments is not supported"
						);
					}
				}
				else {
					throw new UnsupportedOperationException(
							"Matching a parameterized type with multiple type variables"
									+ " in its arguments is not supported"
					);
				}
			}
			else if ( typeArgument instanceof WildcardType ) {
				WildcardType wildcardTypeArgument = (WildcardType) typeArgument;
				Type[] upperBounds = wildcardTypeArgument.getUpperBounds();
				Type[] lowerBounds = wildcardTypeArgument.getLowerBounds();
				if ( upperBounds.length > 1
						|| !Object.class.equals( upperBounds[0] )
						|| lowerBounds.length > 0 ) {
					throw new UnsupportedOperationException(
							"Matching a parameterized type with bounded type arguments is not supported"
					);
				}
			}
			else {
				throw new UnsupportedOperationException(
						"Only type variables and wildcard types are supported"
								+ " as arguments to a parameterized type to match"
				);
			}
		}
		if ( typeVariableIndex == null ) {
			throw new UnsupportedOperationException(
					"Matching a parameterized type without a type variable in its arguments is not supported"
			);
		}
		return new ParameterizedTypeArgumentMatcher( rawTypePattern, typeVariableIndex );
	}
}
