/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;

/**
 * Various utilities to manipulate dates and comply with Elasticsearch date format.
 *
 * @author Guillaume Smet
 */
public final class ElasticsearchDateHelper {

	private static final TimeZone ENCODING_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 9, SignStyle.NORMAL )
			.appendLiteral( "-" )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( "-" )
			.appendValue( DAY_OF_MONTH, 2 )
			.optionalStart()
					.appendLiteral( "T" )
					.appendValue( HOUR_OF_DAY, 2 )
					.appendLiteral( ":" )
					.appendValue( MINUTE_OF_HOUR, 2 )
					.appendLiteral( ":" )
					.appendValue( SECOND_OF_MINUTE, 2 )
					.optionalStart()
							.appendFraction( MILLI_OF_SECOND, 3, 3, true )
					.optionalEnd()
			.optionalEnd()
			.optionalStart()
					.appendOffset( "+HH:MM", "Z" )
			.optionalEnd()
			.toFormatter();

	private ElasticsearchDateHelper() {
		// Not allowed
	}

	public static Date stringToDate(String value) {
		Calendar c = stringToCalendar( value );
		return c.getTime();
	}

	public static String dateToString(Date date) {
		Calendar c = Calendar.getInstance( ENCODING_TIME_ZONE, Locale.ENGLISH );
		c.setTime( date );
		return calendarToString( c );
	}

	public static String calendarToString(Calendar calendar) {
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(
				Instant.ofEpochMilli( calendar.getTimeInMillis() ),
				calendar.getTimeZone().toZoneId()
		);
		return FORMATTER.format( zonedDateTime );
	}

	public static Calendar stringToCalendar(String value) {
		String val = value.trim();
		return FORMATTER.parse( val, ElasticsearchDateHelper::fromTemporal );
	}

	/**
	 * Limit a calendar resolution.
	 *
	 * @param calendar The calendar whose resolution is to be limited
	 * @param resolution The desired resolution of the date to be returned
	 * @return the calendar with all values more precise than {@code resolution} set to 0 or 1
	 *
	 * @see DateTools#round(Date, Resolution)
	 */
	public static Calendar round(Calendar calendar, Resolution resolution) {
		final Calendar calInstance = (Calendar) calendar.clone();
		/*
		 * Make sure we keep the timezone: use a cloned version of the calendar
		 * to set the rounded time on it, not a new calendar instance.
		 */
		calInstance.setTime( DateTools.round( calInstance.getTime(), resolution ) );
		return calInstance;
	}

	private static Calendar fromTemporal(TemporalAccessor temporal) {
		/*
		 * The code below was adapted from ZonedDateTime.from
		 * to also support a zoned date without time, and a local date or date/time.
		 * This is to avoid breaking compatibility with applications developed against
		 * Hibernate Search 5.10.2 and below, which were relying on
		 * javax.xml.bind.DatatypeConverter.parseDateTime for parsing,
		 * and were thus very lenient.
		 */
		LocalDate date = LocalDate.from( temporal );
		LocalTime time = temporal.query( TemporalQueries.localTime() );
		if ( time == null ) {
			time = LocalTime.of( 0, 0, 0 );
		}
		ZoneId zoneId = temporal.query( TemporalQueries.zone() );
		if ( zoneId == null ) {
			zoneId = TimeZone.getDefault().toZoneId();
		}
		return GregorianCalendar.from( ZonedDateTime.of( date, time, zoneId ) );
	}
}
