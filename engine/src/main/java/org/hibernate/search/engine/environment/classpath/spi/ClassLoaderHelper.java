/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Utility class to load instances of other classes by using a fully qualified name,
 * or from a class type.
 * Uses reflection and throws SearchException(s) with proper descriptions of the error,
 * such as the target class is missing a proper constructor, is an interface, is not found...
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Ales Justin
 */
public class ClassLoaderHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private ClassLoaderHelper() {
	}

	/**
	 * Creates an instance of a target class specified by the fully qualified class name using a {@link ClassLoader}
	 * as fallback when the class cannot be found in the context one.
	 *
	 * @param <T> matches the type of targetSuperType: defines the return type
	 * @param targetSuperType the return type of the function, the classNameToLoad will be checked
	 * to be assignable to this type.
	 * @param classNameToLoad a fully qualified class name, whose type is assignable to targetSuperType
	 * @param classResolver the {@link ClassResolver} to use to load classes
	 *
	 * @return a new instance of the type given by {@code classNameToLoad}
	 *
	 * @throws SearchException wrapping other error types with a proper error message for all kind of problems, like
	 * classNotFound, missing proper constructor, wrong type, security errors.
	 */
	public static <T> T instanceFromName(Class<T> targetSuperType,
			String classNameToLoad,
			ClassResolver classResolver) {
		final Class<?> classToLoad;
		final Object instance;
		try {
			classToLoad = classResolver.classForName( classNameToLoad );
			instance = callNoArgConstructor( classToLoad );
		}
		catch (IllegalAccessException | InvocationTargetException | InstantiationException | RuntimeException e) {
			throw log.unableToInstantiateClass( classNameToLoad, Throwables.getFirstNonNullMessage( e ), e );
		}
		return verifySuperTypeCompatibility( targetSuperType, instance, classToLoad );
	}

	/**
	 * Creates an instance of target class.
	 *
	 * @param <T> the type of targetSuperType: defines the return type
	 * @param classToLoad the class to be instantiated
	 *
	 * @return a new instance of classToLoad
	 *
	 * @throws SearchException wrapping other error types with a proper error message for all kind of problems, like
	 * missing proper constructor, securitymanager errors.
	 */
	public static <T> T untypedInstanceFromClass(final Class<T> classToLoad) {
		try {
			return callNoArgConstructor( classToLoad );
		}
		catch (IllegalAccessException | InvocationTargetException | InstantiationException | RuntimeException e) {
			throw log.unableToInstantiateClass( classToLoad.getName(), Throwables.getFirstNonNullMessage( e ), e );
		}
	}

	/**
	 * Verifies that an object instance is implementing a specific interface, or extending a type.
	 *
	 * @param targetSuperType the type to extend, or the interface it should implement
	 * @param instance the object instance to be verified
	 * @param classToLoad the Class of the instance
	 *
	 * @return the same instance
	 */
	@SuppressWarnings("unchecked")
	private static <T> T verifySuperTypeCompatibility(Class<T> targetSuperType, Object instance, Class<?> classToLoad) {
		if ( !targetSuperType.isInstance( instance ) ) {
			throw log.subtypeExpected( classToLoad, targetSuperType );
		}
		else {
			return (T) instance;
		}
	}

	/**
	 * Creates an instance of target class having a Map of strings as constructor parameter.
	 * Most of the Analyzer SPIs provided by Lucene have such a constructor.
	 *
	 * @param <T> the type of targetSuperType: defines the return type
	 * @param targetSuperType the created instance will be checked to be assignable to this type
	 * @param classToLoad the class to be instantiated
	 * @param constructorParameter a Map to be passed to the constructor. The loaded type must have such a constructor.
	 *
	 * @return a new instance of classToLoad
	 *
	 * @throws SearchException wrapping other error types with a proper error message for all kind of problems, like
	 * missing proper constructor, wrong type, security errors.
	 */
	public static <T> T instanceFromClass(Class<T> targetSuperType, Class<?> classToLoad,
			Map<String, String> constructorParameter) {
		final Object instance;
		try {
			instance = callMapArgConstructor( classToLoad, constructorParameter );
		}
		catch (Exception e) {
			throw log.unableToInstantiateClass( classToLoad.getName(), Throwables.getFirstNonNullMessage( e ), e );
		}
		return verifySuperTypeCompatibility( targetSuperType, instance, classToLoad );
	}

	private static void checkInstantiable(Class<?> classToLoad) {
		if ( classToLoad.isInterface() ) {
			throw log.implementationRequired( classToLoad );
		}
	}

	private static <T> T callNoArgConstructor(Class<T> classToLoad)
			throws IllegalAccessException, InvocationTargetException, InstantiationException {
		checkInstantiable( classToLoad );
		try {
			Constructor<T> constructor = classToLoad.getConstructor();
			return constructor.newInstance();
		}
		catch (SecurityException e) {
			throw log.securityManagerLoadingError( classToLoad, e.getMessage(), e );
		}
		catch (NoSuchMethodException e) {
			throw log.noPublicNoArgConstructor( classToLoad );
		}
	}

	private static <T> T callMapArgConstructor(Class<T> classToLoad, Map<String, String> constructorParameter)
			throws IllegalAccessException, InvocationTargetException, InstantiationException {
		if ( constructorParameter == null ) {
			constructorParameter = new HashMap<>( 0 );//can't use the emptyMap singleton as it needs to be mutable
		}
		checkInstantiable( classToLoad );
		try {
			Constructor<T> singleMapConstructor = classToLoad.getConstructor( Map.class );
			return singleMapConstructor.newInstance( constructorParameter );
		}
		catch (SecurityException e) {
			throw log.securityManagerLoadingError( classToLoad, e.getMessage(), e );
		}
		catch (NoSuchMethodException e) {
			throw log.noPublicMapArgConstructor( classToLoad );
		}
	}

	public static <T> Class<? extends T> classForName(Class<T> targetSuperType,
			String classNameToLoad,
			ClassResolver classResolver) {
		final Class<?> clazzDef = classResolver.classForName( classNameToLoad );
		try {
			return clazzDef.asSubclass( targetSuperType );
		}
		catch (ClassCastException cce) {
			throw log.subtypeExpected( clazzDef, targetSuperType );
		}
	}
}
