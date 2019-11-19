/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

@RunWith(Parameterized.class)
@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class ConfigurationPropertyValidMissingValuesTest<T> extends EasyMockSupport {

	@Parameterized.Parameters(name = "{2}")
	public static Object[][] data() {
		return new Object[][] {
				params( KeyContext::asString, "string", "string" ),
				params( KeyContext::asInteger, "42", 42 ),
				params( KeyContext::asLong, "3000000000042", 3000000000042L ),
				params( KeyContext::asBoolean, "true", true ),
				params( KeyContext::asBoolean, "false", false ),
				params(
						c -> c.as( MyPropertyType.class, MyPropertyType::new ),
						"string", new MyPropertyType( "string" )
				)
		};
	}

	private static <T> Object[] params(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		return new Object[] { testedMethod, stringValue, expectedValue };
	}

	private final Function<KeyContext, OptionalPropertyContext<T>> testedMethod;
	private final String stringValue;
	private final T expectedValue;

	private final ConfigurationPropertySource sourceMock = createMock( ConfigurationPropertySource.class );

	public ConfigurationPropertyValidMissingValuesTest(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		this.testedMethod = testedMethod;
		this.stringValue = stringValue;
		this.expectedValue = expectedValue;
	}

	@Test
	public void withDefault() {
		String key = "withDefault";
		ConfigurationProperty<T> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.withDefault( expectedValue )
						.build();

		T result;

		// No value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedValue );

		// String value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( stringValue ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedValue );

		// Typed value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( expectedValue ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedValue );
	}

	@Test
	public void withoutDefault() {
		String key = "withoutDefault";
		ConfigurationProperty<Optional<T>> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.build();

		Optional<T> result;

		// No value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isEmpty();

		// String value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( stringValue ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).isEqualTo( expectedValue );

		// Typed value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( expectedValue ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).isEqualTo( expectedValue );
	}

	@Test
	public void withoutDefault_getAndMap() {
		String key = "withoutDefault_getAndMap";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<T> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.build();

		Function<T, Object> mappingFunction = createMock( Function.class );
		Optional<Object> result;
		Object expectedMappedValue = new Object();

		// No value -> empty
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		replayAll();
		result = property.getAndMap( sourceMock, mappingFunction );
		verifyAll();
		assertThat( result ).isEmpty();

		// Valid value -> no exception, mapping function applied
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( expectedValue ) );
		EasyMock.expect( mappingFunction.apply( expectedValue ) ).andReturn( expectedMappedValue );
		replayAll();
		result = property.getAndMap( sourceMock, mappingFunction );
		verifyAll();
		assertThat( result ).contains( expectedMappedValue );

		// Valid value and mapping function fails
		SimulatedFailure simulatedFailure = new SimulatedFailure( "SIMULATED" );
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( expectedValue ) );
		EasyMock.expect( mappingFunction.apply( expectedValue ) ).andThrow( simulatedFailure );
		EasyMock.expect( sourceMock.resolve( key ) ).andReturn( Optional.of( resolvedKey ) );
		replayAll();
		SubTest.expectException( () -> property.getAndMap( sourceMock, mappingFunction ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Unable to convert configuration property '" + resolvedKey
								+ "' with value '" + expectedValue + "':"
				)
				.hasMessageContaining( simulatedFailure.getMessage() )
				.hasCause( simulatedFailure );
		verifyAll();
	}

	@Test
	public void withoutDefault_getOrThrow() {
		String key = "withoutDefault_getOrThrow";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<T> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.build();

		// No value -> exception
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		EasyMock.expect( sourceMock.resolve( key ) ).andReturn( Optional.of( resolvedKey ) );
		replayAll();
		SubTest.expectException( () -> property.getOrThrow( sourceMock, SimulatedFailure::new ) )
				.assertThrown()
				.isInstanceOf( SimulatedFailure.class )
				.hasMessage( resolvedKey );
		verifyAll();

		// Valid value -> no exception
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( expectedValue ) );
		replayAll();
		T result = property.getOrThrow( sourceMock, SimulatedFailure::new );
		verifyAll();
		assertThat( result ).isEqualTo( expectedValue );
	}

	@Test
	public void withoutDefault_getAndMapOrThrow() {
		String key = "withoutDefault_getAndMapOrThrow";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<T> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.build();

		Function<T, Object> mappingFunction = createMock( Function.class );
		Object result;

		// No value -> exception
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		EasyMock.expect( sourceMock.resolve( key ) ).andReturn( Optional.of( resolvedKey ) );
		replayAll();
		SubTest.expectException( () -> property.getAndMapOrThrow( sourceMock, mappingFunction, SimulatedFailure::new ) )
				.assertThrown()
				.isInstanceOf( SimulatedFailure.class )
				.hasMessage( resolvedKey );
		verifyAll();

		// Valid value -> no exception, mapping function applied
		Object expectedMappedValue = new Object();
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( expectedValue ) );
		EasyMock.expect( mappingFunction.apply( expectedValue ) ).andReturn( expectedMappedValue );
		replayAll();
		result = property.getAndMapOrThrow( sourceMock, mappingFunction, SimulatedFailure::new );
		verifyAll();
		assertThat( result ).isEqualTo( expectedMappedValue );

		// Valid value and mapping function fails
		SimulatedFailure simulatedFailure = new SimulatedFailure( "SIMULATED" );
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( expectedValue ) );
		EasyMock.expect( mappingFunction.apply( expectedValue ) ).andThrow( simulatedFailure );
		EasyMock.expect( sourceMock.resolve( key ) ).andReturn( Optional.of( resolvedKey ) );
		replayAll();
		SubTest.expectException( () -> property.getAndMap( sourceMock, mappingFunction ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Unable to convert configuration property '" + resolvedKey
								+ "' with value '" + expectedValue + "':"
				)
				.hasMessageContaining( simulatedFailure.getMessage() )
				.hasCause( simulatedFailure );
		verifyAll();
	}

	@Test
	public void blankCharacters() {
		String key = "extraBlankCharacters";
		ConfigurationProperty<T> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.withDefault( expectedValue )
						.build();

		T result;

		// Empty string value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "" ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedValue );

		// Blank string value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "    " ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedValue );

		// String value with extra blank characters
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "   " + stringValue + "   " ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedValue );
	}

	@Test
	public void multiValued() {
		String key = "multiValued";
		ConfigurationProperty<Optional<List<T>>> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.multivalued()
						.build();

		Optional<List<T>> result;

		// String value - one
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( stringValue ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue );

		// String value - multiple
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( stringValue + "," + stringValue ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue, expectedValue );

		// Typed value - one
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( createCollection( expectedValue ) ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue );

		// Typed value - multiple
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( createCollection( expectedValue, expectedValue ) ) );
		replayAll();
		result = property.get( sourceMock );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue );
	}

	@SafeVarargs
	private static <T> Collection<T> createCollection(T... values) {
		// Don't create a List, that would be too easy.
		Collection<T> collection = new LinkedHashSet<>();
		Collections.addAll( collection, values );
		return collection;
	}

	private static class MyPropertyType {
		private final String value;

		private MyPropertyType(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || getClass() != obj.getClass() ) {
				return false;
			}
			MyPropertyType other = (MyPropertyType) obj;
			return Objects.equals( value, other.value );
		}

		@Override
		public int hashCode() {
			return Objects.hash( value );
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}

}
