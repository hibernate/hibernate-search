/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;

public final class ConvertUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private ConvertUtils() {
		// Private constructor, do not use
	}

	/**
	 * Convert a property value to a given class, either by casting it if possible
	 * or by treating it as a string to be parsed.
	 *
	 * @param expectedType the expected type
	 * @param parser a parser from String to the expected type
	 * @param value the value to convert (a String)
	 * @param value the value to convert (a String)
	 * @return the converted value
	 * @throws SearchException for invalid values.
	 */
	public static <T> Optional<T> convert(Class<T> expectedType, Function<String, T> parser, Object value) {
		if ( expectedType.isInstance( value ) ) {
			return Optional.of( expectedType.cast( value ) );
		}
		try {
			return optionalTrimmedNonEmpty( (String) value )
					.map( parser );
		}
		catch (RuntimeException e) {
			throw log.invalidPropertyValue( expectedType, e );
		}
	}

	/**
	 * Convert a property value from String to boolean if necessary, allowing only either "true" or "false".
	 *
	 * @param value the value to convert (a Boolean, or a String to be parsed)
	 * @return true if value is "true", false if value is "false"
	 * @throws SearchException for invalid format or values.
	 */
	public static Optional<Boolean> convertBoolean(Object value) {
		if ( value instanceof Boolean ) {
			return Optional.of( (Boolean) value );
		}
		try {
			Optional<String> optionalCleaned = optionalTrimmedNonEmpty( (String) value );
			if ( optionalCleaned.isPresent() ) {
				String cleaned = optionalCleaned.get();
				// avoiding Boolean.valueOf() to have more checks: makes it easy to spot wrong type in cfg.
				if ( "false".equalsIgnoreCase( cleaned ) ) {
					return Optional.of( false );
				}
				else if ( "true".equalsIgnoreCase( cleaned ) ) {
					return Optional.of( true );
				}
			}
		}
		catch (RuntimeException e) {
			throw log.invalidBooleanPropertyValue( e );
		}

		throw log.invalidBooleanPropertyValue( null );
	}

	/**
	 * Convert a property value from String to int if necessary.
	 *
	 * @param value the value to convert (a Number, or a String to be parsed)
	 * @return the converted integer
	 * @throws SearchException for invalid format or values.
	 */
	public static Optional<Integer> convertInteger(Object value) {
		if ( value instanceof Number ) {
			return Optional.of( ( (Number) value ).intValue() );
		}
		try {
			return optionalTrimmedNonEmpty( (String) value )
					.map( Integer::parseInt );
		}
		catch (RuntimeException e) {
			throw log.invalidIntegerPropertyValue( e.getMessage(), e );
		}
	}

	/**
	 * Convert a property value from String to long if necessary.
	 *
	 * @param value the value to convert (a Number, or a String to be parsed)
	 * @return the converted long
	 * @throws SearchException for invalid format or values.
	 */
	public static Optional<Long> convertLong(Object value) {
		if ( value instanceof Number ) {
			return Optional.of( ( (Number) value ).longValue() );
		}
		try {
			return optionalTrimmedNonEmpty( (String) value )
					.map( Long::parseLong );
		}
		catch (RuntimeException e) {
			throw log.invalidLongPropertyValue( e.getMessage(), e );
		}
	}

	public static <T> Optional<List<T>> convertMultiValue(Pattern separatorPattern,
			Function<Object, Optional<T>> elementConverter, Object value) {
		if ( value instanceof Collection ) {
			List<T> result = ( (Collection<?>) value ).stream()
					.map( elementConverter )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.collect( Collectors.toList() );
			return Optional.of( result );
		}
		else if ( value instanceof String ) {
			List<T> result = optionalTrimmedNonEmpty( (String) value )
					.map( separatorPattern::splitAsStream ).orElse( Stream.empty() )
					.map( elementConverter )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.collect( Collectors.toList() );
			return result.isEmpty() ? Optional.empty() : Optional.of( result );
		}
		else {
			throw log.invalidMultiPropertyValue();
		}
	}

	private static Optional<String> optionalTrimmedNonEmpty(String value) {
		if ( value != null && !value.isEmpty() ) {
			String trimmed = value.trim();
			if ( !trimmed.isEmpty() ) {
				return Optional.of( value );
			}
		}
		return Optional.empty();
	}
}
