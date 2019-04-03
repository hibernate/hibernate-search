/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.format.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.asImmutableList;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.search.util.common.AssertionFailure;

/**
 * The default field format provider for Elasticsearch 6.
 * <p>
 * Elasticsearch 6 and below expect Joda time patterns as a format,
 * whose syntax is different from the one used in java.time.
 * <p>
 * In particular:
 * <ul>
 * <li>ZZ means "offset with colon"</li>
 * <li>ZZZ means "zone ID"</li>
 * <li>When parsing, yyyy does not accept more than 4 digits, so we need to add an alternative format with nine "y"</li>
 * <li>When parsing, SSS does not accept more than 3 digits, so we need to add an alternative format with nine "S"</li>
 * </ul>
 * <p>
 * We do not use Elasticsearch built-in formats ("strict_date_time", ...) because they do not always match
 * the formatters used in Hibernate Search:
 * for example they sometimes define an offset without a colon, whereas we send offsets with a colon,
 * or a year-of-era (~ absolute value of the year) instead of year (which can be negative).
 */
public class Elasticsearch6DefaultFieldFormatProvider implements ElasticsearchDefaultFieldFormatProvider {

	private static final Map<Class<? extends TemporalAccessor>, List<String>> ELASTICSEARCH_6_FORMAT_PATTERNS_BY_TYPE;
	static {
		HashMap<Class<? extends TemporalAccessor>, List<String>> map = new HashMap<>();
		map.put( Instant.class, asImmutableList( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'" ) );
		map.put( LocalDate.class, asImmutableList( "yyyy-MM-dd", "yyyyyyyyy-MM-dd" ) );
		map.put( LocalTime.class, asImmutableList( "HH:mm:ss.SSS", "HH:mm:ss.SSSSSSSSS" ) );
		map.put( LocalDateTime.class, asImmutableList( "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS" ) );
		map.put( OffsetTime.class, asImmutableList( "HH:mm:ss.SSSZZ", "HH:mm:ss.SSSSSSSSSZZ" ) );
		map.put( OffsetDateTime.class, asImmutableList( "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ" ) );
		// ZoneRegionId is optional for ZonedDateTime, but we need the offset to handle ambiguous date/times at DST overlap
		map.put( ZonedDateTime.class, asImmutableList( "yyyy-MM-dd'T'HH:mm:ss.SSSZZ'['ZZZ']'", "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ'['ZZZ']'" ) );
		map.put( Year.class, asImmutableList( "yyyy", "yyyyyyyyy" ) );
		map.put( YearMonth.class, asImmutableList( "yyyy-MM", "yyyyyyyyy-MM" ) );
		// MonthDays are formatted as a LocalDate, with a forced year, to support February 29th. See ElasticsearchMonthDayFieldCodec.
		map.put( MonthDay.class, asImmutableList( "yyyy-MM-dd" ) );
		ELASTICSEARCH_6_FORMAT_PATTERNS_BY_TYPE = Collections.unmodifiableMap( map );
	}

	private final Map<Class<? extends TemporalAccessor>, DateTimeFormatter> dateTimeFormatters;

	public Elasticsearch6DefaultFieldFormatProvider() {
		this.dateTimeFormatters = new HashMap<>();

		// We use the same date/time formatters as ES7, but different mapping formats
		for ( Map.Entry<Class<? extends TemporalAccessor>, String> entry :
				Elasticsearch7DefaultFieldFormatProvider.JAVA_TIME_FORMAT_PATTERN_BY_TYPE.entrySet() ) {
			Class<? extends TemporalAccessor> type = entry.getKey();
			String pattern = entry.getValue();
			dateTimeFormatters.put( type, DateTimeFormatter.ofPattern( pattern, Locale.ROOT ) );
		}
	}

	@Override
	public DateTimeFormatter getDefaultDateTimeFormatter(Class<? extends TemporalAccessor> fieldType) {
		DateTimeFormatter result = dateTimeFormatters.get( fieldType );
		if ( result == null ) {
			throw new AssertionFailure( "Unknown date time formatter for " + fieldType );
		}
		return result;
	}

	@Override
	public List<String> getDefaultMappingFormat(Class<? extends TemporalAccessor> fieldType) {
		// ES6 and below expect Joda time patterns as a format, so we can NOT use the pattern directly
		List<String> result = ELASTICSEARCH_6_FORMAT_PATTERNS_BY_TYPE.get( fieldType );
		if ( result == null ) {
			throw new AssertionFailure( "Unknown mapping format for " + fieldType );
		}
		return result;
	}
}
