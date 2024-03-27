/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.bridge.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.Query;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "HSEARCH-1947")
class JavaTimeTest {

	@RegisterExtension
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Sample.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	void testLocalDate() {
		LocalDate date = LocalDate.of( 2012, Month.DECEMBER, 30 );
		Sample sample = new Sample( 1L, "LocalDate example" );
		sample.localDate = date;

		assertThatFieldIsIndexed( "localDate", date, sample );
	}

	@Test
	public void testLocalTime() throws Exception {
		LocalTime time = LocalTime.of( 13, 15, 55, 7 );

		Sample sample = new Sample( 1L, "LocalTime example" );
		sample.localTime = time;

		assertThatFieldIsIndexed( "localTime", time, sample );
	}

	@Test
	public void testLocalDateTime() throws Exception {
		LocalDate date = LocalDate.of( 1998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );

		Sample sample = new Sample( 1L, "LocalDateTime example" );
		sample.localDateTime = dateTime;

		assertThatFieldIsIndexed( "localDateTime", dateTime, sample );
	}

	@Test
	public void testInstant() throws Exception {
		LocalDate date = LocalDate.of( 1998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33, 5 * 1000_000 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );
		Instant instant = dateTime.toInstant( ZoneOffset.UTC );

		Sample sample = new Sample( 1L, "Instant example" );
		sample.instant = instant;

		assertThatFieldIsIndexed( "instant", instant, sample );
	}

	@Test
	public void testDuration() throws Exception {
		Duration value = Duration.ofNanos( Long.MAX_VALUE );

		Sample sample = new Sample( 1L, "Duration example" );
		sample.duration = value;

		assertThatFieldIsIndexed( "duration", value, sample );
	}

	@Test
	public void testPeriod() throws Exception {
		Period value = Period.ZERO;

		Sample sample = new Sample( 1L, "Period example" );
		sample.period = value;

		assertThatFieldIsIndexed( "period", value, sample );
	}

	@Test
	public void testZoneOffset() throws Exception {
		ZoneOffset value = ZoneOffset.MAX;

		Sample sample = new Sample( 1L, "zoneOffset example" );
		sample.zoneOffset = value;

		assertThatFieldIsIndexed( "zoneOffset", value, sample );
	}

	@Test
	public void testZoneId() throws Exception {
		ZoneId value = ZoneId.of( "GMT" );

		Sample sample = new Sample( 1L, "ZoneId example" );
		sample.zoneId = value;

		assertThatFieldIsIndexed( "zoneId", value, sample );
	}

	@Test
	public void testOffsetDateTime() throws Exception {
		/* Elasticsearch only accepts years in the range [-292275054,292278993]
		 */
		OffsetDateTime value = OffsetDateTime.of(
				221998, Month.FEBRUARY.getValue(), 12,
				13, 05, 33, 7,
				ZoneOffset.of( "+01:00" )
		);

		Sample sample = new Sample( 1L, "OffsetDateTime example" );
		sample.offsetDateTime = value;

		assertThatFieldIsIndexed( "offsetDateTime", value, sample );
	}

	@Test
	public void testOffsetTime() throws Exception {
		OffsetTime value = OffsetTime.MIN;

		Sample sample = new Sample( 1L, "OffsetTime example" );
		sample.offsetTime = value;

		assertThatFieldIsIndexed( "offsetTime", value, sample );
	}

	@Test
	public void testZonedDateTime() throws Exception {
		// CET DST rolls back at 2011-10-30 2:59:59 (+02) to 2011-10-30 2:00:00 (+01)
		// Credit: user leonbloy at http://stackoverflow.com/a/18794412/6692043
		LocalDateTime localDateTime = LocalDateTime.of( 2011, 10, 30, 2, 50, 0, 0 );

		ZonedDateTime value = localDateTime.atZone( ZoneId.of( "CET" ) ).withLaterOffsetAtOverlap();

		Sample sample = new Sample( 1L, "ZonedDateTime example" );
		sample.zonedDateTime = value;

		assertThatFieldIsIndexed( "zonedDateTime", value, sample );
	}

	@Test
	public void testYear() throws Exception {
		/* Elasticsearch only accepts years in the range [-292275054,292278993]
		 */
		Year value = Year.of( 292278993 );

		Sample sample = new Sample( 1L, "Year example" );
		sample.year = value;

		assertThatFieldIsIndexed( "year", value, sample );
	}

	@Test
	public void testYearMonth() throws Exception {
		YearMonth value = YearMonth.of( 124, 12 );

		Sample sample = new Sample( 1L, "YearMonth example" );
		sample.yearMonth = value;

		assertThatFieldIsIndexed( "yearMonth", value, sample );
	}

	@Test
	public void testMonthDay() throws Exception {
		MonthDay value = MonthDay.of( 12, 1 );

		Sample sample = new Sample( 1L, "MonthDay example" );
		sample.monthDay = value;

		assertThatFieldIsIndexed( "monthDay", value, sample );
	}

	private void assertThatFieldIsIndexed(String field, Object expectedValue, Sample sample) {
		helper.add( sample, sample.id );

		Query query = queryBuilder().keyword().onField( field ).matching( expectedValue ).createQuery();

		helper.assertThatQuery( query )
				.from( Sample.class )
				.projecting( field )
				.matchesExactlySingleProjections( expectedValue );
	}

	private QueryBuilder queryBuilder() {
		return helper.queryBuilder( Sample.class );
	}

	@Indexed
	private static class Sample {

		public Sample(long id, String description) {
			this.id = id;
			this.description = description;
		}

		@DocumentId
		long id;

		@Field(analyze = Analyze.NO, store = Store.YES)
		String description;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private LocalDate localDate;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private LocalTime localTime;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private LocalDateTime localDateTime;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private Instant instant;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private Duration duration;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private Period period;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private ZoneOffset zoneOffset;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private ZoneId zoneId;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private OffsetDateTime offsetDateTime;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private ZonedDateTime zonedDateTime;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private OffsetTime offsetTime;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private Year year;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private YearMonth yearMonth;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private MonthDay monthDay;
	}

}
