/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.lang.invoke.MethodHandles;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.impl.TimeHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ParseUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// The DateTimeFormatter class does not expose a public constant for the ISO format, so we need to do it ourselves.
	private static final DateTimeFormatter ISO_YEAR = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 10, SignStyle.EXCEEDS_PAD )
			.toFormatter();

	// The DateTimeFormatter class does not expose a public constant for the ISO format, so we need to do it ourselves.
	private static final DateTimeFormatter ISO_YEAR_MONTH = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 10, SignStyle.EXCEEDS_PAD )
			.appendLiteral( '-' )
			.appendValue( MONTH_OF_YEAR, 2 )
			.toFormatter();

	// The DateTimeFormatter class does not expose a public constant for the ISO format, so we need to do it ourselves.
	private static final DateTimeFormatter ISO_MONTH_DAY = new DateTimeFormatterBuilder()
			.appendLiteral( "--" )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter();

	private static final String GEO_POINT_SEPARATOR = ",\\s*";

	private ParseUtils() {
		// Private constructor, do not use
	}

	public static String parseString(String value) {
		return value;
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
			return Year.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( Year.class, value, formatter, e );
		}
	}

	public static YearMonth parseYearMonth(String value) {
		// Using the default ISO format
		DateTimeFormatter formatter = ISO_YEAR_MONTH;

		try {
			return YearMonth.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( YearMonth.class, value, formatter, e );
		}
	}

	public static MonthDay parseMonthDay(String value) {
		// Using the default ISO format
		DateTimeFormatter formatter = ISO_MONTH_DAY;

		try {
			return MonthDay.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.unableToParseTemporal( MonthDay.class, value, formatter, e );
		}
	}

	public static ZoneOffset parseZoneOffset(String value) {
		try {
			return ZoneOffset.of( value );
		}
		catch (DateTimeException e) {
			throw log.invalidStringForType( value, ZoneOffset.class, e );
		}
	}

	public static Period parsePeriod(String value) {
		try {
			return Period.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.invalidStringForType( value, Period.class, e );
		}
	}

	public static Duration parseDuration(String value) {
		try {
			return Duration.parse( value );
		}
		catch (DateTimeParseException e) {
			throw log.invalidStringForType( value, Duration.class, e );
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
}
