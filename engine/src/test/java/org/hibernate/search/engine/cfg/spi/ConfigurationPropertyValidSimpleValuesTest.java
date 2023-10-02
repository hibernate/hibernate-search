/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@SuppressWarnings({ "unchecked", "rawtypes" }) // Raw types are the only way to mock parameterized types
class ConfigurationPropertyValidSimpleValuesTest<T> {

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				params( KeyContext::asString, "string", "string" ),
				params( KeyContext::asIntegerPositiveOrZeroOrNegative, "42", 42 ),
				params( KeyContext::asIntegerPositiveOrZeroOrNegative, "0", 0 ),
				params( KeyContext::asIntegerPositiveOrZeroOrNegative, "-1", -1 ),
				params( KeyContext::asIntegerPositiveOrZero, "42", 42 ),
				params( KeyContext::asIntegerPositiveOrZero, "0", 0 ),
				params( KeyContext::asIntegerStrictlyPositive, "42", 42 ),
				params( KeyContext::asLongPositiveOrZeroOrNegative, "3000000000042", 3000000000042L ),
				params( KeyContext::asBoolean, "true", true ),
				params( KeyContext::asBoolean, "false", false ),
				params(
						c -> c.as( MyPropertyType.class, MyPropertyType::new ),
						"string", new MyPropertyType( "string" )
				)
		);
	}

	private static <T> Arguments params(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		return Arguments.of( testedMethod, stringValue, expectedValue );
	}

	@Mock
	private ConfigurationPropertySource sourceMock;

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void withDefault(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		String key = "withDefault";
		ConfigurationProperty<T> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.withDefault( expectedValue )
						.build();

		T result;

		// No value
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedValue );

		// String value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedValue );

		// Typed value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( expectedValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedValue );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void withoutDefault(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		String key = "withoutDefault";
		ConfigurationProperty<Optional<T>> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.build();

		Optional<T> result;

		// No value
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEmpty();

		// String value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedValue );

		// Typed value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( expectedValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedValue );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void withoutDefault_getOrThrow(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		String key = "withoutDefault_getOrThrow";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<T> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.build();

		// No value -> exception
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.getOrThrow( sourceMock, SimulatedFailure::new ) )
				.isInstanceOf( SearchException.class )
				.hasCauseInstanceOf( SimulatedFailure.class )
				.hasMessageContainingAll( "Invalid value for configuration property '" + resolvedKey + "': ''",
						SimulatedFailure.MESSAGE );
		verifyNoOtherSourceInteractionsAndReset();

		// Valid value -> no exception
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( expectedValue ) );
		T result = property.getOrThrow( sourceMock, SimulatedFailure::new );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedValue );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void multiValued(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		String key = "multiValued";
		ConfigurationProperty<Optional<List<T>>> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.multivalued()
						.build();

		Optional<List<T>> result;

		// String value - one
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result.get() ).containsExactly( expectedValue );

		// String value - multiple
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue + "," + stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue, expectedValue );

		// Typed value - one (not wrapped in a collection)
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( expectedValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue );

		// Typed value - one (wrapped in a collection)
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( createCollection( expectedValue ) ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue );

		// Typed value - multiple
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( createCollection( expectedValue, expectedValue ) ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue, expectedValue );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void substitute_function(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		String key = "substitute_function";
		OptionalConfigurationProperty<T> property = testedMethod.apply( ConfigurationProperty.forKey( key ) )
				.substitute( v -> {
					if ( v instanceof MyEnum ) {
						switch ( (MyEnum) v ) {
							case VALUE1:
								return expectedValue;
							case VALUE2:
								break;
						}
					}
					return v;
				} )
				.build();

		Optional<T> result;

		// Substituted value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE1 ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedValue );

		// No value
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEmpty();

		// String value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedValue );

		// Typed value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( expectedValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedValue );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void substitute_literals(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		String key = "substitute_literals";
		OptionalConfigurationProperty<T> property = testedMethod.apply( ConfigurationProperty.forKey( key ) )
				.substitute( MyEnum.VALUE1, expectedValue )
				.build();

		Optional<T> result;

		// Substituted value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE1 ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedValue );

		// No value
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEmpty();

		// String value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedValue );

		// Typed value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( expectedValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedValue );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void substitute_literals_withDefault(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		String key = "substitute_literals_withDefault";
		ConfigurationProperty<T> property = testedMethod.apply( ConfigurationProperty.forKey( key ) )
				.substitute( MyEnum.VALUE1, expectedValue )
				.withDefault( expectedValue )
				.build();

		T result;

		// Substituted value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE1 ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedValue );

		// No value
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedValue );

		// String value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedValue );

		// Typed value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( expectedValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedValue );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void substitute_literals_multiValued(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String stringValue, T expectedValue) {
		String key = "substitute_literals_multiValued";
		OptionalConfigurationProperty<List<T>> property = testedMethod.apply( ConfigurationProperty.forKey( key ) )
				.substitute( MyEnum.VALUE1, expectedValue )
				.substitute( MyEnum.VALUE2, expectedValue )
				.multivalued()
				.build();

		Optional<List<T>> result;

		// Substituted value - one
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( createCollection( MyEnum.VALUE1 ) ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue );

		// Substituted value - multiple
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( createCollection( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue, expectedValue );

		// Substituted value - multiple and mixed
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( createCollection( expectedValue, MyEnum.VALUE1 ) ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue, expectedValue );

		// String value - one
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue );

		// String value - multiple
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( stringValue + "," + stringValue ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue, expectedValue );

		// Typed value - one
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( createCollection( expectedValue ) ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue );

		// Typed value - multiple
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( createCollection( expectedValue, expectedValue ) ) );
		result = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get() ).containsExactly( expectedValue, expectedValue );
	}

	private void verifyNoOtherSourceInteractionsAndReset() {
		verifyNoMoreInteractions( sourceMock );
		reset( sourceMock );
	}

	@SafeVarargs
	private static <T> Collection<T> createCollection(T... values) {
		// Don't expose a List, that would be too easy.
		// Instead, wrap the list into a collection.
		return Collections.unmodifiableCollection( Arrays.asList( values ) );
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
		public static final String MESSAGE = "The simulated message";

		SimulatedFailure() {
			super( MESSAGE );
		}
	}

	private enum MyEnum {
		VALUE1,
		VALUE2;
	}

}
