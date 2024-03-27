/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.assertj.core.api.InstanceOfAssertFactories;

class ValueReadHandleTest {

	public static List<? extends Arguments> params() {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		return Arrays.asList(
				Arguments.of( ValueHandleFactory.usingMethodHandle( lookup ) ),
				Arguments.of( ValueHandleFactory.usingJavaLangReflect() )
		);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void privateField(ValueHandleFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "privateField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void privateFinalField(ValueHandleFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "privateFinalField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void packagePrivateField(ValueHandleFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "packagePrivateField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void packagePrivateFinalField(ValueHandleFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "packagePrivateFinalField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void protectedField(ValueHandleFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "protectedField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void protectedFinalField(ValueHandleFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "protectedFinalField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void publicField(ValueHandleFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "publicField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void publicFinalField(ValueHandleFactory factory) throws Exception {
		testFieldValueReadHandleSuccess( factory, "publicFinalField" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void privateMethod(ValueHandleFactory factory) throws Exception {
		testMethodValueReadHandleSuccess( factory, "privateMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void packagePrivateMethod(ValueHandleFactory factory) throws Exception {
		testMethodValueReadHandleSuccess( factory, "packagePrivateMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void protectedMethod(ValueHandleFactory factory) throws Exception {
		testMethodValueReadHandleSuccess( factory, "protectedMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void publicMethod(ValueHandleFactory factory) throws Exception {
		testMethodValueReadHandleSuccess( factory, "publicMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void failure_method_error(ValueHandleFactory factory) throws Exception {
		Method method = EntityType.class.getDeclaredMethod( "errorThrowingMethod" );
		setAccessible( method );

		ValueReadHandle<?> valueReadHandle = factory.createForMethod( method );

		EntityType entity = new EntityType();
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.isInstanceOf( SimulatedError.class )
				.hasMessageContaining( "errorThrowingMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void failure_method_runtimeException(ValueHandleFactory factory) throws Exception {
		Method method = EntityType.class.getDeclaredMethod( "runtimeExceptionThrowingMethod" );
		setAccessible( method );

		ValueReadHandle<?> valueReadHandle = factory.createForMethod( method );

		EntityType entity = new EntityType( () -> "toStringResult" );
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Exception while invoking '" + method.toString() + "' on 'toStringResult'"
				)
				.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SimulatedRuntimeException.class )
				.hasMessageContaining( "runtimeExceptionThrowingMethod" );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void failure_method_illegalAccessException(ValueHandleFactory factory) throws Exception {
		assumeFalse(
				factory.getClass().getSimpleName().contains( "MethodHandle" ),
				"Cannot test IllegalAccessException with MethodHandles: "
						+ " if we don't use setAccessible(true), we can't create the handle,"
						+ " and if we do use setAccessible(true), the handle has full access to the field/method."
		);

		Method method = EntityType.class.getDeclaredMethod( "illegalAccessExceptionThrowingMethod" );

		ValueReadHandle<?> valueReadHandle = factory.createForMethod( method );

		EntityType entity = new EntityType( () -> "toStringResult" );
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Exception while invoking '" + method.toString() + "' on 'toStringResult'"
				)
				.extracting( Throwable::getCause ).isInstanceOf( IllegalAccessException.class );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void failure_field_illegalAccessException(ValueHandleFactory factory) throws Exception {
		assumeFalse(
				factory.getClass().getSimpleName().contains( "MethodHandle" ),
				"Cannot test IllegalAccessException with MethodHandles: "
						+ " if we don't use setAccessible(true), we can't create the handle,"
						+ " and if we do use setAccessible(true), the handle has full access to the field/method."
		);

		Field field = EntityType.class.getDeclaredField( "illegalAccessExceptionThrowingField" );

		ValueReadHandle<?> valueReadHandle = factory.createForField( field );

		EntityType entity = new EntityType( () -> "toStringResult" );
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Exception while invoking '" + field.toString() + "' on 'toStringResult'"
				)
				.extracting( Throwable::getCause ).isInstanceOf( IllegalAccessException.class );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void failure_method_secondFailureInToString_runtimeException(ValueHandleFactory factory) throws Exception {
		Method method = EntityType.class.getDeclaredMethod( "runtimeExceptionThrowingMethod" );
		setAccessible( method );

		ValueReadHandle<?> valueReadHandle = factory.createForMethod( method );

		SimulatedRuntimeException toStringRuntimeException = new SimulatedRuntimeException( "toString" );
		EntityType entity = new EntityType( () -> {
			throw toStringRuntimeException;
		} );
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Exception while invoking '" + method.toString()
								+ "' on '<EntityType#toString() threw SimulatedRuntimeException>'"
				)
				.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SimulatedRuntimeException.class )
				.hasMessageContaining( "runtimeExceptionThrowingMethod" )
				.hasSuppressedException( toStringRuntimeException );
	}

	private void testFieldValueReadHandleSuccess(ValueHandleFactory factory, String fieldName)
			throws IllegalAccessException, NoSuchFieldException {
		String expectedValue = fieldName + "Value";
		Field field = EntityType.class.getDeclaredField( fieldName );
		setAccessible( field );
		Field otherField = EntityType.class.getDeclaredField( "otherField" );
		setAccessible( otherField );

		ValueReadHandle<?> valueReadHandle = factory.createForField( field );

		assertThat( valueReadHandle.get( new EntityType() ) ).isEqualTo( expectedValue );

		assertThat( valueReadHandle.toString() )
				.contains( valueReadHandle.getClass().getSimpleName() )
				.contains( field.toString() );

		ValueReadHandle<?> equalValueReadHandle = factory.createForField( field );
		ValueReadHandle<?> differentFieldValueReadHandle = factory.createForField( otherField );
		assertThat( valueReadHandle ).isEqualTo( equalValueReadHandle );
		assertThat( valueReadHandle ).hasSameHashCodeAs( equalValueReadHandle );
		assertThat( valueReadHandle ).isNotEqualTo( differentFieldValueReadHandle );
	}

	private void testMethodValueReadHandleSuccess(ValueHandleFactory factory, String methodName)
			throws IllegalAccessException, NoSuchMethodException {
		String expectedValue = methodName + "Value";
		Method method = EntityType.class.getDeclaredMethod( methodName );
		setAccessible( method );
		Method otherMethod = EntityType.class.getDeclaredMethod( "otherMethod" );
		setAccessible( otherMethod );

		ValueReadHandle<?> valueReadHandle = factory.createForMethod( method );
		assertThat( valueReadHandle.get( new EntityType() ) ).isEqualTo( expectedValue );

		assertThat( valueReadHandle.toString() )
				.contains( valueReadHandle.getClass().getSimpleName() )
				.contains( method.toString() );

		ValueReadHandle<?> equalValueReadHandle = factory.createForMethod( method );
		ValueReadHandle<?> differentMethodValueReadHandle = factory.createForMethod( otherMethod );
		assertThat( valueReadHandle ).isEqualTo( equalValueReadHandle );
		assertThat( valueReadHandle ).hasSameHashCodeAs( equalValueReadHandle );
		assertThat( valueReadHandle ).isNotEqualTo( differentMethodValueReadHandle );
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
		private String illegalAccessExceptionThrowingField = "illegalAccessExceptionThrowingField";

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

		private String illegalAccessExceptionThrowingMethod() {
			return fail( "This method is inaccessible and should not be called" );
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
