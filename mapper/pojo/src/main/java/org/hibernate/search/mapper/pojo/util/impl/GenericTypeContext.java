/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.util.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * A representation of generic parameters and their mapped values.
 * <p>
 * Currently only used as a representation of type variable of a given type and its supertypes,
 * in order to retrieve the type arguments of its supertypes.
 * For instance this class, given the type {@code ArrayList<? extends Map<String, Integer>>},
 * is able to determine that the type argument for {@code List<?>} is {@code ? extends Map<String, Integer>}.
 */
public final class GenericTypeContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Type type;
	private final Map<TypeVariable<?>, Type> typeMappings;

	public GenericTypeContext(Type type) {
		this.type = type;
		this.typeMappings = new HashMap<>();
		populateTypeMappings( typeMappings, type );
	}

	public Optional<Type> resolveTypeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		TypeVariable<? extends Class<?>>[] typeParameters = rawSuperType.getTypeParameters();
		int typeParametersLength = typeParameters.length;
		if ( typeParametersLength == 0 ) {
			throw log.cannotRequestTypeParameterOfUnparameterizedType( type, rawSuperType, typeParameterIndex );
		}
		else if ( typeParametersLength <= typeParameterIndex ) {
			throw log.typeParameterIndexOutOfBound( type, rawSuperType, typeParameterIndex, typeParametersLength );
		}
		else if ( typeParameterIndex < 0 ) {
			throw log.invalidTypeParameterIndex( type, rawSuperType, typeParameterIndex );
		}

		TypeVariable<?> typeVariable = typeParameters[typeParameterIndex];
		Type mappedType = typeMappings.get( typeVariable );
		if ( mappedType == null ) {
			// This type does not extend the given raw supertype
			return Optional.empty();
		}
		else {
			while ( mappedType instanceof TypeVariable && typeVariable != mappedType ) {
				typeVariable = (TypeVariable<?>) mappedType;
				mappedType = typeMappings.get( typeVariable );
			}
			if ( mappedType != null ) {
				return Optional.of( mappedType );
			}
			else {
				// The mappedType type was a type variable external to this type; return the type variable
				return Optional.of( typeVariable );
			}
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

}
