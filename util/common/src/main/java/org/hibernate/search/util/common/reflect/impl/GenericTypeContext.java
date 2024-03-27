/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A representation of generic parameters and their mapped values.
 * <p>
 * Currently only used as a representation of type variables of a given type and its supertypes,
 * in order to retrieve the type arguments of its supertypes.
 * For instance this class, given the type {@code ArrayList<? extends Map<String, Integer>>},
 * is able to determine that the type argument for {@code List<?>} is {@code ? extends Map<String, Integer>}.
 * <p>
 * This class is able to take into account the declaring context when performing resolution.
 * For instance, given the following model:
 * <pre><code>
 * class A&lt;T extends C&gt; {
 *   GenericType&lt;T&gt; propertyOfA;
 * }
 * class B extends A&lt;D&gt; {
 * }
 * class C {
 * }
 * class D extends C {
 * }
 * class GenericType&lt;T&gt; {
 *   T propertyOfGenericType;
 * }
 * </code></pre>
 * ... if an instance of this implementation was used to model the type of {@code B.propertyOfA},
 * then the property {@code B.propertyOfA} would appear to have type {@code List<D>} as one would expect,
 * instead of type {@code T extends C} if we inferred the type solely based on generics information from type {@code A}.
 * <p>
 * This will also be true for more deeply nested references to a type variable,
 * for instance the type of property {@code B.propertyOfA.propertyOfGenericType} will correctly be inferred as D.
 * <p>
 * Note that "type resolution" here only covers the resolution of a type parameter to the corresponding argument,
 * i.e. "shallow" resolution. This class will not build new {@link ParameterizedType} instances to recursively
 * replace type variables with their known value, unlike other libraries such as Guava.
 */
public final class GenericTypeContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Type resolvedType;
	private final GenericTypeContext declaringContext;
	private final Map<TypeVariable<?>, Type> typeMappings;

	public GenericTypeContext(Type type) {
		this( null, null, type );
	}

	public GenericTypeContext(GenericTypeContext declaringContext, Type type) {
		this( declaringContext, null, type );
	}

	public GenericTypeContext(GenericTypeContext declaringContext, GenericTypeContext castBase, Type type) {
		Contracts.assertNotNull( type, "type" );
		if ( declaringContext != null ) {
			this.resolvedType = declaringContext.resolveType( type );
		}
		else {
			this.resolvedType = type;
		}
		this.declaringContext = declaringContext;
		this.typeMappings = new HashMap<>();
		populateTypeMappings( typeMappings, resolvedType );
		if ( castBase != null ) {
			inferTypeMappingsFromCastBase( typeMappings, castBase );
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + "[" + resolvedType.getTypeName() + ", " + declaringContext + "]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		GenericTypeContext that = (GenericTypeContext) o;
		return Objects.equals( resolvedType, that.resolvedType )
				&& Objects.equals( declaringContext, that.declaringContext );
	}

	@Override
	public int hashCode() {
		return Objects.hash( resolvedType, declaringContext );
	}

	public Class<?> rawType() {
		return ReflectionUtils.getRawType( resolvedType );
	}

	public String name() {
		Class<?> rawType = rawType();

		StringBuilder builder = new StringBuilder( rawType.getTypeName() );

		TypeVariable<? extends Class<?>>[] typeParameters = rawType.getTypeParameters();
		if ( typeParameters.length > 0 ) {
			builder.append( '<' );
			for ( int i = 0; i < typeParameters.length; i++ ) {
				if ( i > 0 ) {
					builder.append( ',' );
				}
				builder.append( resolveTypeArgument( rawType, i ).orElse( typeParameters[i] ).getTypeName() );
			}
			builder.append( '>' );
		}

		if ( resolvedType.getTypeName().equals( builder.toString() ) ) {
			return resolvedType.getTypeName();
		}

		builder.insert( 0, resolvedType.getTypeName() + " (" );
		builder.append( ")" );

		return builder.toString();
	}

	public GenericTypeContext declaringContext() {
		return declaringContext;
	}

	public GenericTypeContext castTo(Class<?> target) {
		return new GenericTypeContext( declaringContext, this, target );
	}

	public Optional<Type> resolveTypeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		TypeVariable<? extends Class<?>>[] typeParameters = rawSuperType.getTypeParameters();
		int typeParametersLength = typeParameters.length;
		if ( typeParametersLength == 0 ) {
			throw log.cannotRequestTypeParameterOfUnparameterizedType( resolvedType, rawSuperType, typeParameterIndex );
		}
		else if ( typeParametersLength <= typeParameterIndex ) {
			throw log.typeParameterIndexOutOfBound( resolvedType, rawSuperType, typeParameterIndex, typeParametersLength );
		}
		else if ( typeParameterIndex < 0 ) {
			throw log.invalidTypeParameterIndex( resolvedType, rawSuperType, typeParameterIndex );
		}

		TypeVariable<?> typeVariable = typeParameters[typeParameterIndex];
		if ( !typeMappings.containsKey( typeVariable ) ) {
			// This type does not extend the given raw supertype
			return Optional.empty();
		}
		else {
			return Optional.of( resolveType( typeVariable ) );
		}
	}

	public Optional<Type> resolveArrayElementType() {
		return ReflectionUtils.getArrayElementType( resolvedType )
				.map( this::resolveType );
	}

	private Type resolveType(Type type) {
		Type result = type;
		TypeVariable<?> typeVariable = null;

		while ( result instanceof TypeVariable && result != typeVariable ) {
			typeVariable = (TypeVariable<?>) result;
			if ( typeVariable.getGenericDeclaration() instanceof Class ) {
				result = typeMappings.get( typeVariable );
			}
			else {
				/*
				 * Type variables from non-Class generic declarations (method, constructor, ...)
				 * cannot be resolved statically.
				 * Give up and use the first upper bound.
				 */
				result = typeVariable.getBounds()[0];
			}
		}

		if ( result == null && typeVariable != null ) {
			// The mappedType type was a type variable external to this type
			if ( declaringContext != null ) {
				// Ask the declaring context to resolve it, if possible
				return declaringContext.resolveType( typeVariable );
			}
			else {
				// Give up and return the type variable itself
				return typeVariable;
			}
		}
		else {
			/*
			 * Either the type resolved to a type parameter of a raw type we implement,
			 * or to a non-variable type.
			 * In either case, no need to ask the declaring context to resolve any further.
			 */
			return result;
		}
	}

	private static void populateTypeMappings(Map<TypeVariable<?>, Type> mappings, Type type) {
		if ( type instanceof TypeVariable ) {
			for ( Type upperBound : ( (TypeVariable<?>) type ).getBounds() ) {
				populateTypeMappings( mappings, upperBound );
			}
		}
		else if ( type instanceof WildcardType ) {
			for ( Type upperBound : ( (WildcardType) type ).getUpperBounds() ) {
				populateTypeMappings( mappings, upperBound );
			}
		}
		else if ( type instanceof ParameterizedType ) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Class<?> rawType = ReflectionUtils.getRawType( parameterizedType );

			TypeVariable<? extends Class<?>>[] typeParameters = rawType.getTypeParameters();
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			for ( int i = 0; i < typeParameters.length; ++i ) {
				mappings.put( typeParameters[i], typeArguments[i] );
			}
			populateTypeMappings( mappings, rawType );
		}
		else if ( type instanceof Class ) {
			Class<?> clazz = (Class<?>) type;
			// Make sure to handle raw types as setting the argument to the type parameter itself
			for ( TypeVariable<?> typeParameter : clazz.getTypeParameters() ) {
				mappings.putIfAbsent( typeParameter, typeParameter );
			}
			for ( Type interfaze : clazz.getGenericInterfaces() ) {
				populateTypeMappings( mappings, interfaze );
			}
			Type superClass = clazz.getGenericSuperclass();
			if ( superClass != null ) {
				populateTypeMappings( mappings, superClass );
			}
		}
		else if ( type instanceof GenericArrayType ) {
			// No-op
			// Arrays cannot implement types, so we don't care about those
		}
		else {
			throw new AssertionFailure( "Unexpected java.lang.reflect.Type type: " + type.getClass() );
		}
	}

	// Try to infer subclass type variables from the castBase
	// E.g. if we are creating List<T>, casted from Iterable<Integer>, we can infer that T = Integer.
	// Similarly, if we are creating Iterable<T>, casted from List<Integer>, we can infer that T = Integer.
	private static void inferTypeMappingsFromCastBase(Map<TypeVariable<?>, Type> typeMappings, GenericTypeContext castBase) {
		for ( Map.Entry<TypeVariable<?>, Type> baseMapping : castBase.typeMappings.entrySet() ) {
			TypeVariable<?> baseVariable = baseMapping.getKey();
			Type baseVariableValue = castBase.resolveType( baseVariable );
			if ( baseVariableValue instanceof TypeVariable ) {
				// We don't know the value of that variable in the base; not useful.
				continue;
			}
			Type baseVariableValueInThis = typeMappings.get( baseVariable );
			if ( baseVariableValueInThis instanceof TypeVariable ) {
				// The variable was set to another variable in a subtype
				// Let's try to set that other variable
				TypeVariable<?> subTypeVariable = (TypeVariable<?>) baseVariableValueInThis;
				Type subTypeVariableValueInThis = typeMappings.get( subTypeVariable );
				while ( subTypeVariableValueInThis instanceof TypeVariable
						&& !subTypeVariable.equals( subTypeVariableValueInThis ) ) {
					subTypeVariable = (TypeVariable<?>) subTypeVariableValueInThis;
					subTypeVariableValueInThis = typeMappings.get( subTypeVariable );
				}
				if ( subTypeVariable.equals( subTypeVariableValueInThis ) ) {
					// We successfully inferred the value.
					typeMappings.put( subTypeVariable, baseVariableValue );
				}
				// else: the variable is already set in this,
				// either to the same value as in base (valid cast) or to another value (invalid cast).
			}
		}
	}

}
