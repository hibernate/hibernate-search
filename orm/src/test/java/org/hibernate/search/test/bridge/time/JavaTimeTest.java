/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "HSEARCH-1947")
public class JavaTimeTest extends SearchTestBase {

	@After
	public void deleteEntity() {
		try (org.hibernate.Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			s.delete( s.load( Sample.class, 1L ) );
			s.flush();
			tx.commit();
		}
	}

	@Test
	public void testLocalDate() throws Exception {
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
		OffsetDateTime value = OffsetDateTime.MIN;

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
	public void testYear() throws Exception {
		Year value = Year.of( Year.MAX_VALUE );

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
		try (org.hibernate.Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			s.persist( sample );
			s.flush();
			tx.commit();

			tx = s.beginTransaction();
			final FullTextSession session = Search.getFullTextSession( s );

			Query query = queryBuilder( session ).keyword().onField( field ).ignoreAnalyzer().matching( expectedValue ).createQuery();
			Object[] result = (Object[]) session.createFullTextQuery( query ).setProjection( field ).uniqueResult();

			assertThat( result ).as( "Indexed value for field '" + field + "' not found" ).containsOnly( expectedValue );

			tx.commit();
		}
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	private QueryBuilder queryBuilder(final FullTextSession session) {
		QueryBuilder builder = session.getSearchFactory().buildQueryBuilder().forEntity( Sample.class ).get();
		return builder;
	}

	@Entity
	@Indexed
	static class Sample {

		public Sample() {
		}

		public Sample(long id, String description) {
			this.id = id;
			this.description = description;
		}

		@Id
		@DocumentId
		long id;

		@Field(analyze = Analyze.NO, store = Store.YES)
		String description;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private LocalDate localDate;

		@Column(name = "LOCAL_TIME") // localTime is a special keywork for some db
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
		private OffsetTime offsetTime;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private Year year;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private YearMonth yearMonth;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private MonthDay monthDay;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Sample.class };
	}
}
