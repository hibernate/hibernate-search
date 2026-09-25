/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.Instantiator;
import org.hibernate.search.util.common.AssertionFailure;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ValueCreateHandleTest {

	public static List<? extends Arguments> params() {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		return Arrays.asList(
				Arguments.of( AccessorFactory.lambda( lookup ) ),
				Arguments.of( AccessorFactory.reflection() )
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void privateConstructor(AccessorFactory factory) throws Exception {
		testValueCreateHandleSuccess( PrivateConstructorClass.class, PrivateConstructorClass::getValue, factory );
	}

	public static class PrivateConstructorClass {
		private final String value;

		private PrivateConstructorClass(String value) {
			this.value = value;
		}

		private PrivateConstructorClass(Integer value) {
			throw shouldNotBeUsed();
		}

		public String getValue() {
			return value;
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void packagePrivateConstructor(AccessorFactory factory) throws Exception {
		testValueCreateHandleSuccess( PackagePrivateConstructorClass.class, PackagePrivateConstructorClass::getValue,
				factory
		);
	}

	public static class PackagePrivateConstructorClass {
		private final String value;

		PackagePrivateConstructorClass(String value) {
			this.value = value;
		}

		PackagePrivateConstructorClass(Integer value) {
			throw shouldNotBeUsed();
		}

		public String getValue() {
			return value;
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void protectedConstructor(AccessorFactory factory) throws Exception {
		testValueCreateHandleSuccess( ProtectedConstructorClass.class, ProtectedConstructorClass::getValue, factory );
	}

	public static class ProtectedConstructorClass {
		private final String value;

		protected ProtectedConstructorClass(String value) {
			this.value = value;
		}

		protected ProtectedConstructorClass(Integer value) {
			throw shouldNotBeUsed();
		}

		public String getValue() {
			return value;
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void publicConstructor(AccessorFactory factory) throws Exception {
		testValueCreateHandleSuccess( PublicConstructorClass.class, PublicConstructorClass::getValue, factory );
	}

	public static class PublicConstructorClass {
		private final String value;

		public PublicConstructorClass(String value) {
			this.value = value;
		}

		public PublicConstructorClass(Integer value) {
			throw shouldNotBeUsed();
		}

		public String getValue() {
			return value;
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void failure_error(AccessorFactory factory) throws Exception {
		Constructor<?> constructor = ErrorConstructorClass.class.getDeclaredConstructor( String.class );

		Instantiator<?> instantiator = factory.instantiator( constructor );

		assertThatThrownBy( () -> instantiator.create( "someValue" ) )
				.isInstanceOf( SimulatedError.class )
				.hasMessageContaining( "errorThrowingConstructor" );
	}

	public static class ErrorConstructorClass {
		public ErrorConstructorClass(String value) {
			throw new SimulatedError( "errorThrowingConstructor" );
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void failure_runtimeException(AccessorFactory factory) throws Exception {
		Constructor<?> constructor = RuntimeExceptionConstructorClass.class.getDeclaredConstructor( Object.class, int.class );

		Instantiator<?> instantiator = factory.instantiator( constructor );

		assertThatThrownBy( () -> instantiator.create( "someValue", 42 ) )
				.hasMessageContaining( "runtimeExceptionThrowingConstructor" );
	}

	public static class RuntimeExceptionConstructorClass {
		public RuntimeExceptionConstructorClass(Object value, int otherValue) {
			throw new SimulatedRuntimeException( "runtimeExceptionThrowingConstructor" );
		}
	}

	private <T> void testValueCreateHandleSuccess(Class<T> clazz, Function<T, String> getter,
			AccessorFactory factory)
			throws IllegalAccessException, NoSuchMethodException {
		Constructor<T> constructor = clazz.getDeclaredConstructor( String.class );
		setAccessible( constructor );

		Instantiator<T> instantiator = factory.instantiator( constructor );

		String argument = "someArgument_" + clazz.getName();
		T created = instantiator.create( argument );
		assertThat( getter.apply( created ) ).isEqualTo( argument );
	}

	private static RuntimeException shouldNotBeUsed() {
		return new AssertionFailure( "This method should not be used" );
	}

	private static void setAccessible(Member member) {
		if ( !Modifier.isPublic( member.getModifiers() ) ) {
			( (AccessibleObject) member ).setAccessible( true );
		}
	}

	private static class SimulatedRuntimeException extends RuntimeException {
		public SimulatedRuntimeException(String message) {
			super( message );
		}
	}

	private static class SimulatedError extends Error {
		public SimulatedError(String message) {
			super( message );
		}
	}
}
