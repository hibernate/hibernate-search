/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.TimeHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ParseUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// The DateTimeFormatter class does not expose a public constant for the ISO format, so we need to do it ourselves.
	public static final DateTimeFormatter ISO_YEAR = new DateTimeFormatterBuilder()
			.parseLenient() // Accept "-1" instead of requiring -0001
			.appendValue( YEAR, 4, 10, SignStyle.EXCEEDS_PAD )
			.toFormatter( Locale.ROOT );

	// The DateTimeFormatter class does not expose a public constant for the ISO format, so we need to do it ourselves.
	public static final DateTimeFormatter ISO_YEAR_MONTH = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 10, SignStyle.EXCEEDS_PAD )
			.appendLiteral( '-' )
			.appendValue( MONTH_OF_YEAR, 2 )
			.toFormatter( Locale.ROOT );

	// The DateTimeFormatter class does not expose a public constant for the ISO format, so we need to do it ourselves.
	public static final DateTimeFormatter ISO_MONTH_DAY = new DateTimeFormatterBuilder()
			.appendLiteral( "--" )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter( Locale.ROOT );

	private static final String GEO_POINT_SEPARATOR = ",\\s*";

	private ParseUtils() {
		// Private constructor, do not use
	}

	public static String parseString(String value) {
		return value;
	}

	public static char parseCharacter(String value) {
		if ( value.length() != 1 ) {
			throw log.invalidStringForType( value, Character.class, "", null );
		}
		return value.charAt( 0 );
	}

	public static Boolean parseBoolean(String value) {
		// avoiding Boolean.valueOf() to have more checks: makes it easy to spot wrong type in cfg.
		if ( "false".equalsIgnoreCase( value ) ) {
			return false;
		}
		else if ( "true".equalsIgnoreCase( value ) ) {
			return true;
		}
		throw log.invalidStringForType( value, Boolean.class, "", null );
	}

	public static Integer parseInteger(String value) {
		try {
			return Integer.parseInt( value );
		}
		catch (RuntimeException e) {
			throw log.invalidStringForType( value, Integer.class, e.getMessage(), e );
		}
	}

	public static Long parseLong(String value) {
		try {
			return Long.parseLong( value );
		}
		catch (RuntimeException e) {
			throw log.invalidStringForType( value, Long.class, e.getMessage(), e );
		}
	}

	public static Byte parseByte(String value) {
		try {
			return Byte.parseByte( value );
		}
		catch (RuntimeException e) {
			throw log.invalidStringForType( value, Byte.class, e.getMessage(), e );
		}
	}

	public static Short parseShort(String value) {
		try {
			return Short.parseShort( value );
		}
		catch (RuntimeException e) {
			throw log.invalidStringForType( value, Short.class, e.getMessage(), e );
		}
	}

	public static Float parseFloat(String value) {
		try {
			return Float.parseFloat( value );
		}
		catch (RuntimeException e) {
			throw log.invalidStringForType( value, Float.class, e.getMessage(), e );
		}
	}

	public static Double parseDouble(String value) {
		try {
			return Double.parseDouble( value );
		}
		catch (RuntimeException e) {
			throw log.invalidStringForType( value, Double.class, e.getMessage(), e );
		}
	}

	public static BigDecimal parseBigDecimal(String value) {
		try {
			return new BigDecimal( value );
		}
		catch (RuntimeException e) {
			throw log.invalidStringForType( value, BigDecimal.class, e.getMessage(), e );
		}
	}

	public static BigInteger parseBigInteger(String value) {
		try {
			return new BigInteger( value );
		}
		catch (RuntimeException e) {
			throw log.invalidStringForType( value, BigInteger.class, e.getMessage(), e );
		}
	}

	public static Instant parseInstant(String value) {
		try {
			// Using the default ISO format
			return Instant.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( Instant.class, value, DateTimeFormatter.ISO_INSTANT, e );
		}
	}

	public static LocalDate parseLocalDate(String value) {
		try {
			// Using the default ISO format
			return LocalDate.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( LocalDate.class, value, DateTimeFormatter.ISO_LOCAL_DATE, e );
		}
	}

	public static LocalDateTime parseLocalDateTime(String value) {
		try {
			// Using the default ISO format
			return LocalDateTime.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( LocalDateTime.class, value, DateTimeFormatter.ISO_LOCAL_DATE_TIME, e );
		}
	}

	public static LocalTime parseLocalTime(String value) {
		try {
			// Using the default ISO format
			return LocalTime.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( LocalTime.class, value, DateTimeFormatter.ISO_LOCAL_TIME, e );
		}
	}

	public static OffsetDateTime parseOffsetDateTime(String value) {
		try {
			// Using the default ISO format
			return OffsetDateTime.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( OffsetDateTime.class, value, DateTimeFormatter.ISO_OFFSET_DATE_TIME, e );
		}
	}

	public static OffsetTime parseOffsetTime(String value) {
		try {
			return OffsetTime.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( OffsetTime.class, value, DateTimeFormatter.ISO_OFFSET_TIME, e );
		}
	}

	public static ZonedDateTime parseZonedDateTime(String value) {
		// Using the default ISO format
		DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

		try {
			return TimeHelper.parseZoneDateTime( value, formatter );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( ZonedDateTime.class, value, formatter, e );
		}
	}

	public static Year parseYear(String value) {
		// Using the default ISO format
		DateTimeFormatter formatter = ISO_YEAR;

		try {
			return Year.parse( value, formatter );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( Year.class, value, formatter, e );
		}
	}

	public static YearMonth parseYearMonth(String value) {
		// Using the default ISO format
		DateTimeFormatter formatter = ISO_YEAR_MONTH;

		try {
			return YearMonth.parse( value, formatter );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( YearMonth.class, value, formatter, e );
		}
	}

	public static MonthDay parseMonthDay(String value) {
		// Using the default ISO format
		DateTimeFormatter formatter = ISO_MONTH_DAY;

		try {
			return MonthDay.parse( value, formatter );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( MonthDay.class, value, formatter, e );
		}
	}

	public static ZoneId parseZoneId(String value) {
		try {
			return ZoneId.of( value );
		}
		catch (DateTimeException ex) {
			throw log.invalidStringForType( value, ZoneId.class, ex.getMessage(), ex );
		}
	}

	public static ZoneOffset parseZoneOffset(String value) {
		try {
			return ZoneOffset.of( value );
		}
		catch (DateTimeException e) {
			throw log.invalidStringForType( value, ZoneOffset.class, e.getMessage(), e );
		}
	}

	public static Period parsePeriod(String value) {
		try {
			return Period.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.invalidStringForType( value, Period.class, e.getMessage(), e );
		}
	}

	public static Duration parseDuration(String value) {
		try {
			return Duration.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.invalidStringForType( value, Duration.class, e.getMessage(), e );
		}
	}

	public static UUID parseUUID(String value) {
		try {
			return UUID.fromString( value );
		}
		catch (IllegalArgumentException ex) {
			throw log.invalidStringForType( value, UUID.class, ex.getMessage(), ex );
		}
	}

	public static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
		try {
			return Enum.valueOf( enumType, value );
		}
		catch (IllegalArgumentException ex) {
			throw log.invalidStringForEnum( value, enumType, ex );
		}
	}

	public static GeoPoint parseGeoPoint(String value) {
		// using custom format, ex: '48.633308, 7.759294'
		String[] split = value.split( GEO_POINT_SEPARATOR );
		if ( split.length != 2 ) {
			throw log.unableToParseGeoPoint( value );
		}

		try {
			return GeoPoint.of( Double.parseDouble( split[0] ), Double.parseDouble( split[1] ) );
		}
		catch (NumberFormatException e) {
			throw log.unableToParseGeoPoint( value );
		}
	}

	public static <T> T parseDiscreteValues(T[] allowedValues, Function<T, String> stringRepresentationFunction,
			BiFunction<String, List<String>, RuntimeException> invalidValueFunction,
			String value) {
		final String normalizedValue = value.trim().toLowerCase( Locale.ROOT );

		for ( T candidate : allowedValues ) {
			if ( normalizedValue.equals( stringRepresentationFunction.apply( candidate ) ) ) {
				return candidate;
			}
		}

		throw invalidValueFunction.apply(
				normalizedValue,
				Arrays.stream( allowedValues )
						.map( stringRepresentationFunction )
						.filter( Objects::nonNull )
						.collect( Collectors.toList() )
		);
	}

	public static byte[] parseBytePrimitiveArray(String value) {
		try {
			String[] values = arrayValues( value.trim() );
			byte[] parsed = new byte[values.length];
			for ( int i = 0; i < values.length; i++ ) {
				parsed[i] = parseByte( values[i] );
			}
			return parsed;
		}
		catch (SearchException ex) {
			throw log.invalidStringForType( value, float[].class, ex.getMessage(), ex );
		}
	}

	public static float[] parseFloatPrimitiveArray(String value) {
		try {
			String[] values = arrayValues( value.trim() );
			float[] parsed = new float[values.length];
			for ( int i = 0; i < values.length; i++ ) {
				parsed[i] = parseFloat( values[i] );
			}
			return parsed;
		}
		catch (SearchException ex) {
			throw log.invalidStringForType( value, float[].class, ex.getMessage(), ex );
		}
	}

	private static String[] arrayValues(String value) {
		if ( value.startsWith( "[" ) && value.endsWith( "]" ) ) {
			value = value.substring( 1, value.length() - 1 ).trim();
		}
		return value.split( "[,;\\s]+" );
	}
}
