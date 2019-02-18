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
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

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
	 * @return the converted value
	 * @throws SearchException for invalid values.
	 */
	public static <T> T convert(Class<T> expectedType, Function<String, T> parser, Object value) {
		if ( expectedType.isInstance( value ) ) {
			return expectedType.cast( value );
		}
		try {
			return parser.apply( (String) value );
		}
		catch (RuntimeException e) {
			throw log.invalidPropertyValue( expectedType, e.getMessage(), e );
		}
	}

	/**
	 * Convert a property value from String to boolean if necessary, allowing only either "true" or "false".
	 *
	 * @param value the value to convert (a Boolean, or a String to be parsed)
	 * @return true if value is "true", false if value is "false"
	 * @throws SearchException for invalid format or values.
	 */
	public static Boolean convertBoolean(Object value) {
		try {
			if ( value instanceof Boolean ) {
				return (Boolean) value;
			}
			if ( value instanceof String ) {
				String string = (String) value;
				// avoiding Boolean.valueOf() to have more checks: makes it easy to spot wrong type in cfg.
				if ( "false".equalsIgnoreCase( string ) ) {
					return false;
				}
				else if ( "true".equalsIgnoreCase( string ) ) {
					return true;
				}
			}
		}
		catch (RuntimeException e) {
			throw log.invalidBooleanPropertyValue( e.getMessage(), e );
		}

		throw log.invalidBooleanPropertyValue( "", null );
	}

	/**
	 * Convert a property value from String to int if necessary.
	 *
	 * @param value the value to convert (a Number, or a String to be parsed)
	 * @return the converted integer
	 * @throws SearchException for invalid format or values.
	 */
	public static Integer convertInteger(Object value) {
		try {
			if ( value instanceof Number ) {
				return ( (Number) value ).intValue();
			}
			if ( value instanceof String ) {
				return Integer.parseInt( (String) value );
			}
		}
		catch (RuntimeException e) {
			throw log.invalidIntegerPropertyValue( e.getMessage(), e );
		}

		throw log.invalidIntegerPropertyValue( "", null );
	}

	/**
	 * Convert a property value from String to long if necessary.
	 *
	 * @param value the value to convert (a Number, or a String to be parsed)
	 * @return the converted long
	 * @throws SearchException for invalid format or values.
	 */
	public static Long convertLong(Object value) {
		try {
			if ( value instanceof Number ) {
				return ( (Number) value ).longValue();
			}
			if ( value instanceof String ) {
				return Long.parseLong( (String) value );
			}
		}
		catch (RuntimeException e) {
			throw log.invalidLongPropertyValue( e.getMessage(), e );
		}

		throw log.invalidLongPropertyValue( "", null );
	}

	public static <T> BeanReference<? extends T> convertBeanReference(Class<T> expectedType, Object value) {
		try {
			if ( expectedType.isInstance( value ) ) {
				return BeanReference.ofInstance( expectedType.cast( value ) );
			}
			if ( value instanceof BeanReference ) {
				return ( (BeanReference<?>) value ).asSubTypeOf( expectedType );
			}
			if ( value instanceof Class ) {
				Class<?> castedValue = (Class<?>) value;
				if ( !expectedType.isAssignableFrom( castedValue ) ) {
					throw log.invalidBeanType( expectedType, castedValue );
				}
				@SuppressWarnings("unchecked") // Checked using reflection just above
				Class<? extends T> castedValueAsChildType = (Class<? extends T>) value;
				return BeanReference.of( castedValueAsChildType );
			}
			if ( value instanceof String ) {
				return BeanReference.of( expectedType, (String) value );
			}
		}
		catch (RuntimeException e) {
			throw log.invalidBeanReferencePropertyValue( expectedType, e.getMessage(), e );
		}

		throw log.invalidBeanReferencePropertyValue( expectedType, "", null );
	}

	public static <T> List<T> convertMultiValue(Pattern separatorPattern,
			Function<Object, T> elementConverter, Object value) {
		if ( value instanceof Collection ) {
			return ( (Collection<?>) value ).stream()
					.map( ConvertUtils::trimIfString )
					.filter( Objects::nonNull )
					.map( elementConverter )
					.collect( Collectors.toList() );
		}
		else if ( value instanceof String ) {
			return separatorPattern.splitAsStream( (String) value )
					.map( ConvertUtils::trimIfString )
					.filter( Objects::nonNull )
					.map( elementConverter )
					.collect( Collectors.toList() );
		}
		else {
			throw log.invalidMultiPropertyValue();
		}
	}

	static Object trimIfString(Object value) {
		if ( value instanceof String ) {
			String stringValue = (String) value;
			String trimmed = stringValue.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}
		else {
			return value;
		}
	}
}
