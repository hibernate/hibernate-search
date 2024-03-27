/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.format.impl;

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
 * The default field format provider for Elasticsearch 7.
 * <p>
 * Elasticsearch 7 and above accept java.time patterns as a format,
 * so we set the format to the same pattern used internally in Hibernate Search.
 * <p>
 * We do not use Elasticsearch built-in formats ("strict_date_time", ...) because they do not always match
 * the formatters used in Hibernate Search:
 * for example they sometimes define an offset without a colon, whereas we send offsets with a colon,
 * or a year-of-era (~ absolute value of the year) instead of year (which can be negative).
 */
public class Elasticsearch7DefaultFieldFormatProvider implements ElasticsearchDefaultFieldFormatProvider {

	static final Map<Class<? extends TemporalAccessor>, String> JAVA_TIME_FORMAT_PATTERN_BY_TYPE;
	static {
		HashMap<Class<? extends TemporalAccessor>, String> map = new HashMap<>();
		map.put( Instant.class, "uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSZZZZZ" );
		map.put( LocalDate.class, "uuuu-MM-dd" );
		map.put( LocalTime.class, "HH:mm:ss.SSSSSSSSS" );
		map.put( LocalDateTime.class, "uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS" );
		map.put( OffsetTime.class, "HH:mm:ss.SSSSSSSSSZZZZZ" );
		map.put( OffsetDateTime.class, "uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSZZZZZ" );
		// ZoneRegionId is optional for ZonedDateTime, but we need the offset to handle ambiguous date/times at DST overlap
		map.put( ZonedDateTime.class, "uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSZZZZZ'['VV']'" );
		map.put( Year.class, "uuuu" );
		map.put( YearMonth.class, "uuuu-MM" );
		// MonthDays are formatted as a LocalDate, with a forced year, to support February 29th. See ElasticsearchMonthDayFieldCodec.
		map.put( MonthDay.class, "uuuu-MM-dd" );
		JAVA_TIME_FORMAT_PATTERN_BY_TYPE = Collections.unmodifiableMap( map );
	}

	private final Map<Class<? extends TemporalAccessor>, DateTimeFormatter> dateTimeFormatters;
	private final Map<Class<? extends TemporalAccessor>, List<String>> mappingFormats;

	public Elasticsearch7DefaultFieldFormatProvider() {
		this.dateTimeFormatters = new HashMap<>();
		this.mappingFormats = new HashMap<>();

		for ( Map.Entry<Class<? extends TemporalAccessor>, String> entry : JAVA_TIME_FORMAT_PATTERN_BY_TYPE.entrySet() ) {
			Class<? extends TemporalAccessor> type = entry.getKey();
			String pattern = entry.getValue();
			dateTimeFormatters.put( type, DateTimeFormatter.ofPattern( pattern, Locale.ROOT ) );
			// ES7 and above expect java.time patterns as a format, so we can use the pattern directly
			mappingFormats.put( type, Collections.singletonList( pattern ) );
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
		List<String> result = mappingFormats.get( fieldType );
		if ( result == null ) {
			throw new AssertionFailure( "Unknown mapping format for " + fieldType );
		}
		return result;
	}
}
