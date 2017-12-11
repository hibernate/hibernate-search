/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.bridge.spi.FunctionBridge;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public final class FunctionBridgeUtil {

	private FunctionBridgeUtil() {
	}

	@SuppressWarnings("unchecked")
	public static <T> Optional<Class<T>> inferParameterType(FunctionBridge<T, ?> functionBridge) {
		return getFunctionBridgeTypeArgument( functionBridge, 0 ).map( c -> (Class<T>) c );
	}

	@SuppressWarnings("unchecked")
	public static <R> Optional<Class<R>> inferReturnType(FunctionBridge<?, R> functionBridge) {
		return getFunctionBridgeTypeArgument( functionBridge, 1 ).map( c -> (Class<R>) c );
	}

	private static Optional<Class<?>> getFunctionBridgeTypeArgument(FunctionBridge<?, ?> functionBridge, int index) {
		Class<?> bridgeType = functionBridge.getClass();

		return getImplementedParameterizedInterface( bridgeType, FunctionBridge.class )
				.map( type -> type.getActualTypeArguments()[index] )
				.map( FunctionBridgeUtil::getRawType );
	}

	private static Optional<ParameterizedType> getImplementedParameterizedInterface(Class<?> implementationType, Class<?> targetInterface) {
		for ( Type interfaze : implementationType.getGenericInterfaces() ) {
			if ( interfaze instanceof ParameterizedType ) {
				ParameterizedType parameterizedInterface = (ParameterizedType) interfaze;
				if ( targetInterface.equals( parameterizedInterface.getRawType() ) ) {
					return Optional.of( parameterizedInterface );
				}
			}
		}
		Class<?> superClass = implementationType.getSuperclass();
		if ( superClass != null && !Object.class.equals( superClass ) ) {
			return getImplementedParameterizedInterface( superClass, targetInterface );
		}
		else {
			return Optional.empty();
		}
	}

	private static Class<?> getRawType(Type type) {
		if ( type instanceof Class ) {
			return (Class<?>) type;
		}
		if ( type instanceof ParameterizedType ) {
			return getRawType( ( (ParameterizedType) type ).getRawType() );
		}
		throw new SearchException( "Could not get the raw type for " + type );
	}
}
