/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeFalse;

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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.InstanceOfAssertFactories;

@RunWith(Parameterized.class)
public class ValueReadHandleTest {

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> params() {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		return Arrays.asList( new Object[][] {
				{ ValueHandleFactory.usingMethodHandle( lookup ) },
				{ ValueHandleFactory.usingJavaLangReflect() }
		} );
	}

	private final ValueHandleFactory factory;

	public ValueReadHandleTest(ValueHandleFactory factory) {
		this.factory = factory;
	}

	@Test
	public void privateField() throws Exception {
		testFieldValueReadHandleSuccess( "privateField" );
	}

	@Test
	public void privateFinalField() throws Exception {
		testFieldValueReadHandleSuccess( "privateFinalField" );
	}

	@Test
	public void packagePrivateField() throws Exception {
		testFieldValueReadHandleSuccess( "packagePrivateField" );
	}

	@Test
	public void packagePrivateFinalField() throws Exception {
		testFieldValueReadHandleSuccess( "packagePrivateFinalField" );
	}

	@Test
	public void protectedField() throws Exception {
		testFieldValueReadHandleSuccess( "protectedField" );
	}

	@Test
	public void protectedFinalField() throws Exception {
		testFieldValueReadHandleSuccess( "protectedFinalField" );
	}

	@Test
	public void publicField() throws Exception {
		testFieldValueReadHandleSuccess( "publicField" );
	}

	@Test
	public void publicFinalField() throws Exception {
		testFieldValueReadHandleSuccess( "publicFinalField" );
	}

	@Test
	public void privateMethod() throws Exception {
		testMethodValueReadHandleSuccess( "privateMethod" );
	}

	@Test
	public void packagePrivateMethod() throws Exception {
		testMethodValueReadHandleSuccess( "packagePrivateMethod" );
	}

	@Test
	public void protectedMethod() throws Exception {
		testMethodValueReadHandleSuccess( "protectedMethod" );
	}

	@Test
	public void publicMethod() throws Exception {
		testMethodValueReadHandleSuccess( "publicMethod" );
	}

	@Test
	public void failure_method_error() throws Exception {
		Method method = EntityType.class.getDeclaredMethod( "errorThrowingMethod" );
		setAccessible( method );

		ValueReadHandle<?> valueReadHandle = factory.createForMethod( method );

		EntityType entity = new EntityType();
		assertThatThrownBy( () -> valueReadHandle.get( entity ) )
				.isInstanceOf( SimulatedError.class )
				.hasMessageContaining( "errorThrowingMethod" );
	}

	@Test
	public void failure_method_runtimeException() throws Exception {
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

	@Test
	public void failure_method_illegalAccessException() throws Exception {
		assumeFalse(
				"Cannot test IllegalAccessException with MethodHandles: "
						+ " if we don't use setAccessible(true), we can't create the handle,"
						+ " and if we do use setAccessible(true), the handle has full access to the field/method.",
				factory.getClass().getSimpleName().contains( "MethodHandle" )
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

	@Test
	public void failure_field_illegalAccessException() throws Exception {
		assumeFalse(
				"Cannot test IllegalAccessException with MethodHandles: "
						+ " if we don't use setAccessible(true), we can't create the handle,"
						+ " and if we do use setAccessible(true), the handle has full access to the field/method.",
				factory.getClass().getSimpleName().contains( "MethodHandle" )
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

	@Test
	public void failure_method_secondFailureInToString_runtimeException() throws Exception {
		Method method = EntityType.class.getDeclaredMethod( "runtimeExceptionThrowingMethod" );
		setAccessible( method );

		ValueReadHandle<?> valueReadHandle = factory.createForMethod( method );

		SimulatedRuntimeException toStringRuntimeException = new SimulatedRuntimeException( "toString" );
		EntityType entity = new EntityType( () -> { throw toStringRuntimeException; } );
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

	private void testFieldValueReadHandleSuccess(String fieldName) throws IllegalAccessException, NoSuchFieldException {
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
		assertThat( valueReadHandle.hashCode() ).isEqualTo( equalValueReadHandle.hashCode() );
		assertThat( valueReadHandle ).isNotEqualTo( differentFieldValueReadHandle );
	}

	private void testMethodValueReadHandleSuccess(String methodName) throws IllegalAccessException, NoSuchMethodException {
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
		assertThat( valueReadHandle.hashCode() ).isEqualTo( equalValueReadHandle.hashCode() );
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
