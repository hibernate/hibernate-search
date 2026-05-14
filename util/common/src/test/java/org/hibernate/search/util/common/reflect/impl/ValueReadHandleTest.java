/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.accessor.HibernateAccessorFactory;
import org.hibernate.accessor.HibernateAccessorValueReader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ValueReadHandleTest {

	public static List<? extends Arguments> params() {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		return Arrays.asList(
				Arguments.of( HibernateAccessorFactory.lambda( lookup ) ),
				Arguments.of( HibernateAccessorFactory.reflection() )
		);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void privateField(HibernateAccessorFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "privateField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void privateFinalField(HibernateAccessorFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "privateFinalField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void packagePrivateField(HibernateAccessorFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "packagePrivateField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void packagePrivateFinalField(HibernateAccessorFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "packagePrivateFinalField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void protectedField(HibernateAccessorFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "protectedField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void protectedFinalField(HibernateAccessorFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "protectedFinalField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void publicField(HibernateAccessorFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "publicField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void publicFinalField(HibernateAccessorFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "publicFinalField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void privateMethod(HibernateAccessorFactory factory) throws Exception {
		testMethodValueReadHandleSuccess( factory, "privateMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void packagePrivateMethod(HibernateAccessorFactory factory) throws Exception {
		testMethodValueReadHandleSuccess( factory, "packagePrivateMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void protectedMethod(HibernateAccessorFactory factory) throws Exception {
		testMethodValueReadHandleSuccess( factory, "protectedMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void publicMethod(HibernateAccessorFactory factory) throws Exception {
		testMethodValueReadHandleSuccess( factory, "publicMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void failure_method_error(HibernateAccessorFactory factory) throws Exception {
		Method method = EntityType.class.getDeclaredMethod( "errorThrowingMethod" );
		setAccessible( method );

		HibernateAccessorValueReader<?> valueReadHandle = factory.valueReader( method );

		EntityType entity = new EntityType();
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.isInstanceOf( SimulatedError.class )
				.hasMessageContaining( "errorThrowingMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void failure_method_runtimeException(HibernateAccessorFactory factory) throws Exception {
		Method method = EntityType.class.getDeclaredMethod( "runtimeExceptionThrowingMethod" );
		setAccessible( method );

		HibernateAccessorValueReader<?> valueReadHandle = factory.valueReader( method );

		EntityType entity = new EntityType( () -> "toStringResult" );
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.hasMessageContaining( "runtimeExceptionThrowingMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void failure_method_secondFailureInToString_runtimeException(HibernateAccessorFactory factory) throws Exception {
		Method method = EntityType.class.getDeclaredMethod( "runtimeExceptionThrowingMethod" );
		setAccessible( method );

		HibernateAccessorValueReader<?> valueReadHandle = factory.valueReader( method );

		SimulatedRuntimeException toStringRuntimeException = new SimulatedRuntimeException( "toString" );
		EntityType entity = new EntityType( () -> {
			throw toStringRuntimeException;
		} );
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.isInstanceOfAny( SimulatedRuntimeException.class, RuntimeException.class );
	}

	private void testFieldValueReadHandleSuccess(HibernateAccessorFactory factory, String fieldName)
			throws IllegalAccessException, NoSuchFieldException {
		String expectedValue = fieldName + "Value";
		Field field = EntityType.class.getDeclaredField( fieldName );
		setAccessible( field );

		HibernateAccessorValueReader<?> valueReadHandle = factory.valueReader( field );

		assertThat( valueReadHandle.get( new EntityType() ) ).isEqualTo( expectedValue );
	}

	private void testMethodValueReadHandleSuccess(HibernateAccessorFactory factory, String methodName)
			throws IllegalAccessException, NoSuchMethodException {
		String expectedValue = methodName + "Value";
		Method method = EntityType.class.getDeclaredMethod( methodName );
		setAccessible( method );

		HibernateAccessorValueReader<?> valueReadHandle = factory.valueReader( method );
		assertThat( valueReadHandle.get( new EntityType() ) ).isEqualTo( expectedValue );
	}

	private static void setAccessible(Member member) {
		if ( !Modifier.isPublic( member.getModifiers() ) ) {
			( (AccessibleObject) member ).setAccessible( true );
		}
	}

	private static class EntityType {
		private final Supplier<String> toString;

		private String privateField = "privateFieldValue";
		private final String privateFinalField = "privateFinalFieldValue";
		String packagePrivateField = "packagePrivateFieldValue";
		final String packagePrivateFinalField = "packagePrivateFinalFieldValue";
		protected String protectedField = "protectedFieldValue";
		protected final String protectedFinalField = "protectedFinalFieldValue";
		public String publicField = "publicFieldValue";
		public final String publicFinalField = "publicFinalFieldValue";
		private String otherField;

		private EntityType() {
			this.toString = () -> fail( "Unexpected call to 'toString()'" );
		}

		private EntityType(Supplier<String> toString) {
			this.toString = toString;
		}

		@Override
		public String toString() {
			return toString.get();
		}

		private String privateMethod() {
			return "privateMethodValue";
		}

		String packagePrivateMethod() {
			return "packagePrivateMethodValue";
		}

		protected String protectedMethod() {
			return "protectedMethodValue";
		}

		public String publicMethod() {
			return "publicMethodValue";
		}

		public String otherMethod() {
			return "otherMethod";
		}

		public String runtimeExceptionThrowingMethod() {
			throw new SimulatedRuntimeException( "runtimeExceptionThrowingMethod" );
		}

		public String errorThrowingMethod() {
			throw new SimulatedError( "errorThrowingMethod" );
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
