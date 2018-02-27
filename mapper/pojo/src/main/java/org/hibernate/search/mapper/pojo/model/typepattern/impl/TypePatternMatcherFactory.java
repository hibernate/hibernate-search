/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.hibernate.search.util.AssertionFailure;

public class TypePatternMatcherFactory {

	/**
	 * @param typeToMatch The type to match. Not all types are accepted.
	 * @param resultType The result type. Not all types are accepted.
	 * @return A type pattern matcher matching subtypes of {@code typeToMatch}
	 * and returning {@code resultType}.
	 * @throws UnsupportedOperationException If this factory does not support creating a type pattern matcher
	 * for the given types.
	 */
	public TypePatternMatcher create(Type typeToMatch, Type resultType) {
		if ( typeToMatch instanceof TypeVariable ) {
			throw new UnsupportedOperationException( "Matching a type variable is not supported" );
		}
		else if ( typeToMatch instanceof WildcardType ) {
			throw new UnsupportedOperationException( "Matching a wildcard type is not supported" );
		}
		else if ( typeToMatch instanceof ParameterizedType ) {
			ParameterizedType parameterizedTypeToMatch = (ParameterizedType) typeToMatch;
			Class<?> rawTypeToMatch = (Class<?>) parameterizedTypeToMatch.getRawType();
			Type[] typeArguments = parameterizedTypeToMatch.getActualTypeArguments();

			Integer typeVariableIndex = null;
			for ( int i = 0; i < typeArguments.length; i++ ) {
				Type typeArgument = typeArguments[i];
				if ( typeArgument instanceof TypeVariable ) {
					if ( typeVariableIndex == null ) {
						typeVariableIndex = i;
						TypeVariable<?> typeVariable = (TypeVariable<?>) typeArgument;
						Type[] upperBounds = typeVariable.getBounds();
						if ( !resultType.equals( typeVariable ) ) {
							throw new UnsupportedOperationException(
									"Returning anything other than the type variable when matching parameterized types"
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
					if ( upperBounds.length > 1 || !Object.class.equals( upperBounds[0] )
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
			return new ParameterizedTypeArgumentMatcher( rawTypeToMatch, typeVariableIndex );
		}
		else if ( typeToMatch instanceof Class ) {
			if ( !( resultType instanceof Class ) ) {
				throw new UnsupportedOperationException(
						"Returning a non-raw result type when matching a raw type is not supported"
				);
			}
			return new ExactTypeMatcher( (Class<?>) typeToMatch, (Class<?>) resultType );
		}
		else if ( typeToMatch instanceof GenericArrayType ) {
			GenericArrayType arrayTypeToMatch = (GenericArrayType) typeToMatch;
			if ( !( resultType instanceof TypeVariable )
					|| !arrayTypeToMatch.getGenericComponentType().equals( resultType ) ) {
				throw new UnsupportedOperationException(
						"Returning anything other than the array element type when matching array types"
								+ " is not supported"
				);
			}
			TypeVariable<?> resultTypeVariable = (TypeVariable<?>) resultType;
			Type[] upperBounds = resultTypeVariable.getBounds();
			if ( upperBounds.length > 1 || !Object.class.equals( upperBounds[0] ) ) {
				throw new UnsupportedOperationException(
						"Matching types with bounded type variables is not supported"
				);
			}
			return new ArrayElementTypeMatcher();
		}
		else {
			throw new AssertionFailure( "Unexpected java.lang.reflect.Type type: " + typeToMatch.getClass() );
		}
	}
}
