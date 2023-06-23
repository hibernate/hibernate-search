/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@RunWith(Parameterized.class)
@SuppressWarnings({ "unchecked", "rawtypes" }) // Raw types are the only way to mock parameterized types
public class ConfigurationPropertyInvalidSimpleValuesTest<T> {

	@Parameterized.Parameters(name = "{1}")
	public static Object[][] data() {
		return new Object[][] {
				params(
						KeyContext::asIntegerPositiveOrZeroOrNegative, "foo", 42,
						"Invalid Integer value: expected either a Number or a String that can be parsed into an Integer.",
						"For input string: \"foo\""
				),
				params(
						KeyContext::asIntegerPositiveOrZero, "foo", 42,
						"Invalid Integer value: expected either a Number or a String that can be parsed into an Integer.",
						"For input string: \"foo\""
				),
				params(
						KeyContext::asIntegerPositiveOrZero, "-1", 42,
						"Invalid Integer value: expected either a Number or a String that can be parsed into an Integer.",
						"", // No particular prefix for validation failures
						"'value' must be positive or zero"
				),
				params(
						KeyContext::asIntegerStrictlyPositive, "foo", 42,
						"Invalid Integer value: expected either a Number or a String that can be parsed into an Integer.",
						"For input string: \"foo\""
				),
				params(
						KeyContext::asIntegerStrictlyPositive, "0", 42,
						"Invalid Integer value: expected either a Number or a String that can be parsed into an Integer.",
						"", // No particular prefix for validation failures
						"'value' must be strictly positive"
				),
				params(
						KeyContext::asIntegerStrictlyPositive, "-1", 42,
						"Invalid Integer value: expected either a Number or a String that can be parsed into an Integer.",
						"", // No particular prefix for validation failures
						"'value' must be strictly positive"
				),
				params(
						KeyContext::asLongPositiveOrZeroOrNegative, "bar", 42L,
						"Invalid Long value: expected either a Number or a String that can be parsed into a Long.",
						"For input string: \"bar\""
				),
				params(
						KeyContext::asBoolean, "foobar", true,
						"Invalid Boolean value: expected either a Boolean, the String 'true' or the String 'false'.",
						"" // Nothing particular to add in case of a parsing error
				),
				params(
						c -> c.as( MyPropertyType.class, MyPropertyType::new ),
						MyPropertyType.INVALID_VALUE, new MyPropertyType( "foobar" ),
						"Invalid value: expected either an instance of '" + MyPropertyType.class.getName()
								+ "' or a String that can be parsed into that type.",
						MyPropertyType.INVALID_VALUE_ERROR_MESSAGE
				)
		};
	}

	private static <T> Object[] params(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String invalidStringValue, T validValue,
			String expectedInvalidValueCommonMessagePrefix,
			String expectedInvalidStringMessage) {
		return params( testedMethod, invalidStringValue, validValue, expectedInvalidValueCommonMessagePrefix,
				expectedInvalidValueCommonMessagePrefix, expectedInvalidStringMessage );
	}

	private static <T> Object[] params(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String invalidStringValue, T validValue,
			String expectedInvalidValueTypeMessagePrefix,
			String expectedInvalidValueStringMessagePrefix,
			String expectedInvalidStringMessage) {
		return new Object[] {
				testedMethod,
				invalidStringValue,
				validValue,
				expectedInvalidValueTypeMessagePrefix,
				expectedInvalidValueStringMessagePrefix,
				expectedInvalidStringMessage
		};
	}

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private ConfigurationPropertySource sourceMock;

	private final Function<KeyContext, OptionalPropertyContext<T>> testedMethod;
	private final String invalidStringValue;
	private final T validValue;
	private final String expectedInvalidValueTypeMessagePrefix;
	private final String expectedInvalidValueStringMessagePrefix;
	private final String expectedInvalidStringMessage;

	public ConfigurationPropertyInvalidSimpleValuesTest(Function<KeyContext, OptionalPropertyContext<T>> testedMethod,
			String invalidStringValue, T validValue,
			String expectedInvalidValueTypeMessagePrefix,
			String expectedInvalidValueStringMessagePrefix,
			String expectedInvalidStringMessage) {
		this.testedMethod = testedMethod;
		this.invalidStringValue = invalidStringValue;
		this.validValue = validValue;
		this.expectedInvalidValueTypeMessagePrefix = expectedInvalidValueTypeMessagePrefix;
		this.expectedInvalidValueStringMessagePrefix = expectedInvalidValueStringMessagePrefix;
		this.expectedInvalidStringMessage = expectedInvalidStringMessage;
	}

	@Test
	public void withDefault() {
		String key = "withDefault";
		String resolvedKey = "some.prefix." + key;
		ConfigurationProperty<T> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.withDefault( validValue )
						.build();

		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( invalidStringValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + invalidStringValue + "'."
				)
				.hasMessageContaining( expectedInvalidValueStringMessagePrefix )
				.hasMessageContaining( expectedInvalidStringMessage );
		verifyNoOtherSourceInteractionsAndReset();

		InvalidType invalidTypeValue = new InvalidType();
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( invalidTypeValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + invalidTypeValue + "'."
				)
				.hasMessageContaining( expectedInvalidValueTypeMessagePrefix );
		verifyNoOtherSourceInteractionsAndReset();
	}

	@Test
	public void withoutDefault() {
		String key = "withoutDefault";
		String resolvedKey = "some.prefix." + key;
		ConfigurationProperty<Optional<T>> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.build();

		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( invalidStringValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + invalidStringValue + "'."
				)
				.hasMessageContaining( expectedInvalidValueStringMessagePrefix )
				.hasMessageContaining( expectedInvalidStringMessage );
		verifyNoOtherSourceInteractionsAndReset();

		InvalidType invalidTypeValue = new InvalidType();
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( invalidTypeValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + invalidTypeValue + "'."
				)
				.hasMessageContaining( expectedInvalidValueTypeMessagePrefix );
		verifyNoOtherSourceInteractionsAndReset();
	}

	@Test
	public void multiValued() {
		String key = "multiValued";
		String resolvedKey = "some.prefix." + key;
		ConfigurationProperty<Optional<List<T>>> property =
				testedMethod.apply(
						ConfigurationProperty.forKey( key )
				)
						.multivalued()
						.build();

		// String value - one
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( invalidStringValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + invalidStringValue + "'."
				)
				.hasMessageContaining( expectedInvalidValueStringMessagePrefix )
				.hasMessageContaining( expectedInvalidStringMessage );
		verifyNoOtherSourceInteractionsAndReset();

		// String value - multiple
		String commaSeparatedStringValue = invalidStringValue + "," + invalidStringValue;
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( commaSeparatedStringValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + commaSeparatedStringValue + "'."
				)
				.hasMessageContaining( expectedInvalidValueStringMessagePrefix )
				.hasMessageContaining( expectedInvalidStringMessage );
		verifyNoOtherSourceInteractionsAndReset();

		// Invalid type value in collection
		Collection<InvalidType> invalidTypeValueCollection = createCollection( new InvalidType() );
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( invalidTypeValueCollection ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + invalidTypeValueCollection + "'."
				)
				.hasMessageContaining( expectedInvalidValueTypeMessagePrefix );
		verifyNoOtherSourceInteractionsAndReset();

		// Invalid type value instead of collection
		InvalidType invalidTypeValue = new InvalidType();
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( invalidTypeValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContainingAll(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + invalidTypeValue + "'.",
						"Invalid multi value: expected either a single value of the correct type, a Collection, or a String",
						"interpreting as a single value failed with the following exception",
						expectedInvalidValueTypeMessagePrefix )
				.hasCauseInstanceOf( SearchException.class );
		verifyNoOtherSourceInteractionsAndReset();
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

	private static class InvalidType {
		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}

	private static class MyPropertyType {
		private static final String INVALID_VALUE = "invalid";
		private static final String INVALID_VALUE_ERROR_MESSAGE = "Value '" + INVALID_VALUE + "' is forbidden";

		private final String value;

		private MyPropertyType(String value) {
			if ( INVALID_VALUE.equals( value ) ) {
				throw new IllegalArgumentException( INVALID_VALUE_ERROR_MESSAGE );
			}
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
}
