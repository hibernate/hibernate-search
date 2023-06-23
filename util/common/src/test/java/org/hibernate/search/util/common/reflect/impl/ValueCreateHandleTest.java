/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeFalse;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.InstanceOfAssertFactories;

@RunWith(Parameterized.class)
public class ValueCreateHandleTest {

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> params() {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		return Arrays.asList( new Object[][] {
				{ ValueHandleFactory.usingMethodHandle( lookup ) },
				{ ValueHandleFactory.usingJavaLangReflect() }
		} );
	}

	private final ValueHandleFactory factory;

	public ValueCreateHandleTest(ValueHandleFactory factory) {
		this.factory = factory;
	}

	@Test
	public void privateConstructor() throws Exception {
		testValueCreateHandleSuccess( PrivateConstructorClass.class, PrivateConstructorClass::getValue );
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

	@Test
	public void packagePrivateConstructor() throws Exception {
		testValueCreateHandleSuccess( PackagePrivateConstructorClass.class, PackagePrivateConstructorClass::getValue );
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

	@Test
	public void protectedConstructor() throws Exception {
		testValueCreateHandleSuccess( ProtectedConstructorClass.class, ProtectedConstructorClass::getValue );
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

	@Test
	public void publicConstructor() throws Exception {
		testValueCreateHandleSuccess( PublicConstructorClass.class, PublicConstructorClass::getValue );
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

	@Test
	public void failure_error() throws Exception {
		Constructor<?> constructor = ErrorConstructorClass.class.getDeclaredConstructor( String.class );

		ValueCreateHandle<?> valueCreateHandle = factory.createForConstructor( constructor );

		assertThatThrownBy( () -> valueCreateHandle.create( "someValue" ) )
				.isInstanceOf( SimulatedError.class )
				.hasMessageContaining( "errorThrowingConstructor" );
	}

	public static class ErrorConstructorClass {
		public ErrorConstructorClass(String value) {
			throw new SimulatedError( "errorThrowingConstructor" );
		}
	}

	@Test
	public void failure_runtimeException() throws Exception {
		Constructor<?> constructor = RuntimeExceptionConstructorClass.class.getDeclaredConstructor( Object.class, int.class );

		ValueCreateHandle<?> valueCreateHandle = factory.createForConstructor( constructor );

		assertThatThrownBy( () -> valueCreateHandle.create( "someValue", 42 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Exception while invoking '" + constructor.toString() + "' with arguments [someValue, 42]",
						"runtimeExceptionThrowingConstructor"
				)
				.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SimulatedRuntimeException.class )
				.hasMessageContaining( "runtimeExceptionThrowingConstructor" );
	}

	public static class RuntimeExceptionConstructorClass {
		public RuntimeExceptionConstructorClass(Object value, int otherValue) {
			throw new SimulatedRuntimeException( "runtimeExceptionThrowingConstructor" );
		}
	}

	@Test
	public void failure_illegalAccessException() throws Exception {
		assumeFalse(
				"Cannot test IllegalAccessException with MethodHandles: "
						+ " if we don't use setAccessible(true), we can't create the handle,"
						+ " and if we do use setAccessible(true), the handle has full access to the constructor.",
				factory.getClass().getSimpleName().contains( "MethodHandle" )
		);

		Constructor<?> constructor = IllegalAccessExceptionConstructorClass.class.getDeclaredConstructor( String.class );

		ValueCreateHandle<?> valueCreateHandle = factory.createForConstructor( constructor );

		assertThatThrownBy( () -> valueCreateHandle.create( "someValue" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Exception while invoking '" + constructor.toString() + "' with arguments [someValue]"
				)
				.extracting( Throwable::getCause ).isInstanceOf( IllegalAccessException.class );
	}

	public static class IllegalAccessExceptionConstructorClass {
		private IllegalAccessExceptionConstructorClass(String value) {
			// The exception should be thrown simply because the constructor is private
		}
	}

	@Test
	public void failure_instantiationException() throws Exception {
		Constructor<?> constructor = InstantiationExceptionConstructorClass.class.getDeclaredConstructor( String.class );

		ValueCreateHandle<?> valueCreateHandle = factory.createForConstructor( constructor );

		assertThatThrownBy( () -> valueCreateHandle.create( "someValue" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Exception while invoking '" + constructor.toString() + "' with arguments [someValue]"
				)
				.extracting( Throwable::getCause ).isInstanceOf( InstantiationException.class );
	}

	public abstract static class InstantiationExceptionConstructorClass {
		public InstantiationExceptionConstructorClass(String value) {
			// The exception should be thrown simply because the class is abstract
		}
	}

	@Test
	public void failure_secondFailureInToString_runtimeException() throws Exception {
		Constructor<?> constructor = RuntimeExceptionConstructorClass.class.getDeclaredConstructor( Object.class, int.class );

		ValueCreateHandle<?> valueCreateHandle = factory.createForConstructor( constructor );

		SimulatedRuntimeException toStringRuntimeException = new SimulatedRuntimeException( "toString" );
		Object objectWhoseToStringFails = new CustomToStringType( () -> {
			throw toStringRuntimeException;
		} );

		assertThatThrownBy( () -> valueCreateHandle.create( objectWhoseToStringFails, 42 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Exception while invoking '" + constructor
								+ "' with arguments [<CustomToStringType#toString() threw SimulatedRuntimeException>, 42]",
						"runtimeExceptionThrowingConstructor"
				)
				.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SimulatedRuntimeException.class )
				.hasMessageContaining( "runtimeExceptionThrowingConstructor" )
				.hasSuppressedException( toStringRuntimeException );
	}

	private <T> void testValueCreateHandleSuccess(Class<T> clazz, Function<T, String> getter)
			throws IllegalAccessException, NoSuchMethodException {
		Constructor<T> constructor = clazz.getDeclaredConstructor( String.class );
		setAccessible( constructor );
		Constructor<T> otherConstructor = clazz.getDeclaredConstructor( Integer.class );
		setAccessible( otherConstructor );

		ValueCreateHandle<T> valueCreateHandle = factory.createForConstructor( constructor );

		String argument = "someArgument_" + clazz.getName();
		T created = valueCreateHandle.create( argument );
		assertThat( getter.apply( created ) ).isEqualTo( argument );

		assertThat( valueCreateHandle.toString() )
				.contains( valueCreateHandle.getClass().getSimpleName() )
				.contains( constructor.toString() );

		ValueCreateHandle<T> equalValueCreateHandle = factory.createForConstructor( constructor );
		ValueCreateHandle<T> differentConstructorValueCreateHandle = factory.createForConstructor( otherConstructor );
		assertThat( valueCreateHandle ).isEqualTo( equalValueCreateHandle );
		assertThat( valueCreateHandle.hashCode() ).isEqualTo( equalValueCreateHandle.hashCode() );
		assertThat( valueCreateHandle ).isNotEqualTo( differentConstructorValueCreateHandle );
	}

	private static RuntimeException shouldNotBeUsed() {
		return new AssertionFailure( "This method should not be used" );
	}

	private static void setAccessible(Member member) {
		if ( !Modifier.isPublic( member.getModifiers() ) ) {
			( (AccessibleObject) member ).setAccessible( true );
		}
	}

	private static class CustomToStringType {
		private final Supplier<String> toString;

		private CustomToStringType(Supplier<String> toString) {
			this.toString = toString;
		}

		@Override
		public String toString() {
			return toString.get();
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
