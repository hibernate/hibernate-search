/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A representation of a method defined in a test class in order to be called from test infrastructure
 * (e.g. a {@link org.junit.rules.TestRule}).
 */
class TestPluggableMethod<T> {

	public static <T> List<TestPluggableMethod<T>> createAll(Class<? extends Annotation> annotationClass,
			Class<?> testClass, Class<T> expectedReturnType, List<ArgumentKey<?>> availableKeys) {
		List<TestPluggableMethod<T>> result = new ArrayList<>();
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		collectAllSetupMethods( lookup, annotationClass, result, testClass, expectedReturnType, availableKeys );
		return result;
	}

	private static <T> void collectAllSetupMethods(MethodHandles.Lookup lookup, Class<? extends Annotation> annotationClass,
			List<TestPluggableMethod<T>> collector, Class<?> testClass,
			Class<T> expectedReturnType, List<ArgumentKey<?>> availableKeys) {
		Class<?> superClass = testClass.getSuperclass();
		if ( superClass != Object.class ) {
			collectAllSetupMethods( lookup, annotationClass, collector, superClass, expectedReturnType, availableKeys );
		}
		for ( Method method : testClass.getDeclaredMethods() ) {
			if ( method.isAnnotationPresent( annotationClass ) ) {
				collector.add( create( lookup, annotationClass, method, expectedReturnType, availableKeys ) );
			}
		}
	}

	private static <T> TestPluggableMethod<T> create(MethodHandles.Lookup lookup, Class<? extends Annotation> annotationClass,
			Method method, Class<T> expectedReturnType, List<ArgumentKey<?>> availableKeys) {
		if ( !Modifier.isPublic( method.getModifiers() ) ) {
			throw new IllegalStateException(
					"Method " + method + ", annotated with " + annotationClass.getName() + ", must be public." );
		}
		if ( Modifier.isStatic( method.getModifiers() ) ) {
			throw new IllegalStateException(
					"Method " + method + ", annotated with " + annotationClass.getName() + ", must be non-static." );
		}
		if ( !expectedReturnType.isAssignableFrom( method.getReturnType() ) ) {
			throw new IllegalStateException(
					"Method " + method + ", annotated with " + annotationClass.getName() + ", must return type "
							+ expectedReturnType );
		}
		MethodHandle setupMethod;
		try {
			setupMethod = lookup.unreflect( method );
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(
					"Method " + method + ", annotated with " + annotationClass.getName() + ", must be accessible from " + lookup
							+ ".",
					e
			);
		}
		List<ArgumentKey<?>> argumentKeys = new ArrayList<>();
		for ( Class<?> parameterType : method.getParameterTypes() ) {
			ArgumentKey<?> matchingKey = null;
			for ( ArgumentKey<?> key : availableKeys ) {
				if ( parameterType.isAssignableFrom( key.type ) ) {
					matchingKey = key;
					break;
				}
			}
			if ( matchingKey == null ) {
				throw new IllegalStateException(
						"Method " + method + " has a parameter of type " + parameterType.getName() + ", which isn't supported."
								+ " Supported parameter types: "
								+ availableKeys.stream().map( k -> k.type.getName() ).collect( Collectors.toList() ) );
			}
			argumentKeys.add( matchingKey );
		}
		return new TestPluggableMethod<>( setupMethod, expectedReturnType, argumentKeys );
	}

	private final MethodHandle setupMethod;
	private final Class<T> expectedReturnType;
	private final List<ArgumentKey<?>> argumentKeys;

	private TestPluggableMethod(MethodHandle setupMethod, Class<T> expectedReturnType,
			List<ArgumentKey<?>> argumentKeys) {
		this.setupMethod = setupMethod;
		this.expectedReturnType = expectedReturnType;
		this.argumentKeys = argumentKeys;
	}

	public T call(Object testInstance, Map<ArgumentKey<?>, Object> context) {
		Object[] args = new Object[1 + argumentKeys.size()];
		args[0] = testInstance;
		int i = 1;
		for ( ArgumentKey<?> argumentKey : argumentKeys ) {
			args[i++] = context.get( argumentKey );
		}
		try {
			return expectedReturnType.cast( setupMethod.invokeWithArguments( args ) );
		}
		catch (Throwable t) {
			throw new Error(
					"Failed to call " + setupMethod + " with arguments " + Arrays.toString( args )
							+ ": " + t.getMessage(),
					t
			);
		}
	}

	public static final class ArgumentKey<T> {
		public final Class<T> type;
		public final String name;

		public ArgumentKey(Class<T> type, String name) {
			this.type = type;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ArgumentKey<?> that = (ArgumentKey<?>) o;
			return Objects.equals( type, that.type ) && Objects.equals( name, that.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( type, name );
		}
	}
}
