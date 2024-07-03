/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.hibernate.search.engine.spatial.GeoPoint;

import org.junit.jupiter.api.Test;

class FormatUtilsTest {

	@Test
	void formatBigDecimal() {
		assertThat( FormatUtils.format( BigDecimal.ONE ) ).isEqualTo( "1" );
		assertThat( FormatUtils.format( BigDecimal.ZERO ) ).isEqualTo( "0" );
		assertThat( FormatUtils.format( BigDecimal.valueOf( 100.001 ) ) ).isEqualTo( "100.001" );
		assertThat( FormatUtils.format( BigDecimal.valueOf( -100.001 ) ) ).isEqualTo( "-100.001" );
	}

	@Test
	void formatBigInteger() {
		assertThat( FormatUtils.format( BigInteger.ONE ) ).isEqualTo( "1" );
		assertThat( FormatUtils.format( BigInteger.ZERO ) ).isEqualTo( "0" );
		assertThat( FormatUtils.format( BigInteger.valueOf( 100L ) ) ).isEqualTo( "100" );
		assertThat( FormatUtils.format( BigInteger.valueOf( -100L ) ) ).isEqualTo( "-100" );
	}

	@Test
	void formatBoolean() {
		assertThat( FormatUtils.format( Boolean.TRUE ) ).isEqualTo( "true" );
		assertThat( FormatUtils.format( Boolean.FALSE ) ).isEqualTo( "false" );
	}

	@Test
	void formatByte() {
		assertThat( FormatUtils.format( (byte) 1 ) ).isEqualTo( "1" );
		assertThat( FormatUtils.format( (byte) 0 ) ).isEqualTo( "0" );
		assertThat( FormatUtils.format( (byte) 100 ) ).isEqualTo( "100" );
		assertThat( FormatUtils.format( (byte) -100 ) ).isEqualTo( "-100" );
	}

	@Test
	void formatDouble() {
		assertThat( FormatUtils.format( 1.0 ) ).isEqualTo( "1.0" );
		assertThat( FormatUtils.format( 0.0 ) ).isEqualTo( "0.0" );
		assertThat( FormatUtils.format( 100.001 ) ).isEqualTo( "100.001" );
		assertThat( FormatUtils.format( -100.001 ) ).isEqualTo( "-100.001" );
	}

	@Test
	void formatFloat() {
		assertThat( FormatUtils.format( 1.0f ) ).isEqualTo( "1.0" );
		assertThat( FormatUtils.format( 0.0f ) ).isEqualTo( "0.0" );
		assertThat( FormatUtils.format( 100.001f ) ).isEqualTo( "100.001" );
		assertThat( FormatUtils.format( -100.001f ) ).isEqualTo( "-100.001" );
	}

	@Test
	void formatGeoPoint() {
		assertThat( FormatUtils.format( GeoPoint.of( 1.0, 1.0 ) ) ).isEqualTo( "1.0, 1.0" );
		assertThat( FormatUtils.format( GeoPoint.of( 0.0, 0.0 ) ) ).isEqualTo( "0.0, 0.0" );
		assertThat( FormatUtils.format( GeoPoint.of( 100.001, 100.001 ) ) ).isEqualTo( "100.001, 100.001" );
		assertThat( FormatUtils.format( GeoPoint.of( -100.001, -100.001 ) ) ).isEqualTo( "-100.001, -100.001" );
	}

	@Test
	void formatInstant() {
		assertThat( FormatUtils.format( Instant.ofEpochMilli( 1 ) ) ).isEqualTo( "1970-01-01T00:00:00.001Z" );
		assertThat( FormatUtils.format( Instant.ofEpochMilli( 0 ) ) ).isEqualTo( "1970-01-01T00:00:00Z" );
		assertThat( FormatUtils.format( Instant.ofEpochMilli( 100_000_000 ) ) ).isEqualTo( "1970-01-02T03:46:40Z" );
		assertThat( FormatUtils.format( Instant.ofEpochMilli( 100_000_000_000L ) ) ).isEqualTo( "1973-03-03T09:46:40Z" );
	}

	@Test
	void formatInteger() {
		assertThat( FormatUtils.format( 1 ) ).isEqualTo( "1" );
		assertThat( FormatUtils.format( 0 ) ).isEqualTo( "0" );
		assertThat( FormatUtils.format( 100 ) ).isEqualTo( "100" );
		assertThat( FormatUtils.format( -100 ) ).isEqualTo( "-100" );
	}

	@Test
	void formatLocalDate() {
		assertThat( FormatUtils.format( LocalDate.of( 2000, 1, 4 ) ) ).isEqualTo( "2000-01-04" );
		assertThat( FormatUtils.format( LocalDate.of( 2000, 2, 3 ) ) ).isEqualTo( "2000-02-03" );
		assertThat( FormatUtils.format( LocalDate.of( 20000, 3, 2 ) ) ).isEqualTo( "+20000-03-02" );
		assertThat( FormatUtils.format( LocalDate.of( 5000, 4, 1 ) ) ).isEqualTo( "5000-04-01" );
	}

	@Test
	void formatLocalDateTime() {
		assertThat( FormatUtils.format( LocalDateTime.of( 2000, 1, 4, 0, 0 ) ) ).isEqualTo( "2000-01-04T00:00:00" );
		assertThat( FormatUtils.format( LocalDateTime.of( 2000, 2, 3, 1, 1 ) ) ).isEqualTo( "2000-02-03T01:01:00" );
		assertThat( FormatUtils.format( LocalDateTime.of( 20000, 3, 2, 10, 20 ) ) ).isEqualTo( "+20000-03-02T10:20:00" );
		assertThat( FormatUtils.format( LocalDateTime.of( 5000, 4, 1, 10, 20 ) ) ).isEqualTo( "5000-04-01T10:20:00" );
	}


	@Test
	void formatLocalTime() {
		assertThat( FormatUtils.format( LocalTime.of( 1, 4, 0, 0 ) ) ).isEqualTo( "01:04:00" );
		assertThat( FormatUtils.format( LocalTime.of( 2, 3, 1, 1 ) ) ).isEqualTo( "02:03:01.000000001" );
		assertThat( FormatUtils.format( LocalTime.of( 3, 2, 10, 20 ) ) ).isEqualTo( "03:02:10.00000002" );
		assertThat( FormatUtils.format( LocalTime.of( 4, 1, 10, 20 ) ) ).isEqualTo( "04:01:10.00000002" );
	}

	@Test
	void formatLong() {
		assertThat( FormatUtils.format( 1L ) ).isEqualTo( "1" );
		assertThat( FormatUtils.format( 0L ) ).isEqualTo( "0" );
		assertThat( FormatUtils.format( 100L ) ).isEqualTo( "100" );
		assertThat( FormatUtils.format( -100L ) ).isEqualTo( "-100" );
	}

	@Test
	void formatMothDay() {
		assertThat( FormatUtils.format( MonthDay.of( 1, 4 ) ) ).isEqualTo( "--01-04" );
		assertThat( FormatUtils.format( MonthDay.of( 2, 3 ) ) ).isEqualTo( "--02-03" );
		assertThat( FormatUtils.format( MonthDay.of( 3, 2 ) ) ).isEqualTo( "--03-02" );
		assertThat( FormatUtils.format( MonthDay.of( 4, 1 ) ) ).isEqualTo( "--04-01" );
	}

	@Test
	void formatOffsetDateTime() {
		assertThat( FormatUtils.format( LocalDateTime.of( 2000, 1, 4, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ) ) )
				.isEqualTo( "2000-01-04T00:00:00+01:00" );
		assertThat( FormatUtils.format( LocalDateTime.of( 2000, 2, 3, 1, 1 ).atOffset( ZoneOffset.ofHoursMinutes( 1, 30 ) ) ) )
				.isEqualTo( "2000-02-03T01:01:00+01:30" );
		assertThat( FormatUtils.format( LocalDateTime.of( 20000, 3, 2, 10, 20 ).atOffset( ZoneOffset.ofHours( -1 ) ) ) )
				.isEqualTo( "+20000-03-02T10:20:00-01:00" );
		assertThat( FormatUtils.format( LocalDateTime.of( 5000, 4, 1, 10, 20 ).atOffset( ZoneOffset.ofHours( 10 ) ) ) )
				.isEqualTo( "5000-04-01T10:20:00+10:00" );
	}

	@Test
	void formatOffsetTime() {
		assertThat( FormatUtils.format( LocalTime.of( 1, 4, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ) ) )
				.isEqualTo( "01:04:00+01:00" );
		assertThat( FormatUtils.format( LocalTime.of( 2, 3, 1, 1 ).atOffset( ZoneOffset.ofHoursMinutes( 1, 30 ) ) ) )
				.isEqualTo( "02:03:01.000000001+01:30" );
		assertThat( FormatUtils.format( LocalTime.of( 3, 2, 10, 20 ).atOffset( ZoneOffset.ofHours( -1 ) ) ) )
				.isEqualTo( "03:02:10.00000002-01:00" );
		assertThat( FormatUtils.format( LocalTime.of( 4, 1, 10, 20 ).atOffset( ZoneOffset.ofHours( 10 ) ) ) )
				.isEqualTo( "04:01:10.00000002+10:00" );
	}

	@Test
	void formatShort() {
		assertThat( FormatUtils.format( (short) 1 ) ).isEqualTo( "1" );
		assertThat( FormatUtils.format( (short) 0 ) ).isEqualTo( "0" );
		assertThat( FormatUtils.format( (short) 100 ) ).isEqualTo( "100" );
		assertThat( FormatUtils.format( (short) -100 ) ).isEqualTo( "-100" );
	}

	@Test
	void formatString() {
		assertThat( FormatUtils.format( "(byte) 1" ) ).isEqualTo( "(byte) 1" );
		assertThat( FormatUtils.format( "(byte) 0" ) ).isEqualTo( "(byte) 0" );
		assertThat( FormatUtils.format( "(byte) 100" ) ).isEqualTo( "(byte) 100" );
		assertThat( FormatUtils.format( "(byte) -100" ) ).isEqualTo( "(byte) -100" );
		assertThat( FormatUtils.format( ">_<" ) ).isEqualTo( ">_<" );
	}

	@Test
	void formatYear() {
		assertThat( FormatUtils.format( Year.of( 1400 ) ) ).isEqualTo( "1400" );
		assertThat( FormatUtils.format( Year.of( 2300 ) ) ).isEqualTo( "2300" );
		assertThat( FormatUtils.format( Year.of( 32000 ) ) ).isEqualTo( "+32000" );
		assertThat( FormatUtils.format( Year.of( 4100 ) ) ).isEqualTo( "4100" );
	}

	@Test
	void formatYearMonth() {
		assertThat( FormatUtils.format( YearMonth.of( 1400, 1 ) ) ).isEqualTo( "1400-01" );
		assertThat( FormatUtils.format( YearMonth.of( 2300, 2 ) ) ).isEqualTo( "2300-02" );
		assertThat( FormatUtils.format( YearMonth.of( 32000, 3 ) ) ).isEqualTo( "+32000-03" );
		assertThat( FormatUtils.format( YearMonth.of( 4100, 4 ) ) ).isEqualTo( "4100-04" );
	}

	@Test
	void formatZonedDateTime() {
		assertThat( FormatUtils.format( LocalDateTime.of( 2000, 1, 4, 0, 0 ).atZone( ZoneOffset.ofHours( 1 ) ) ) )
				.isEqualTo( "2000-01-04T00:00:00+01:00" );
		assertThat( FormatUtils.format( LocalDateTime.of( 2000, 2, 3, 1, 1 ).atZone( ZoneOffset.ofHoursMinutes( 1, 30 ) ) ) )
				.isEqualTo( "2000-02-03T01:01:00+01:30" );
		assertThat( FormatUtils.format( LocalDateTime.of( 20000, 3, 2, 10, 20 ).atZone( ZoneOffset.ofHours( -1 ) ) ) )
				.isEqualTo( "+20000-03-02T10:20:00-01:00" );
		assertThat( FormatUtils.format( LocalDateTime.of( 5000, 4, 1, 10, 20 ).atZone( ZoneOffset.ofHours( 10 ) ) ) )
				.isEqualTo( "5000-04-01T10:20:00+10:00" );
	}

}
