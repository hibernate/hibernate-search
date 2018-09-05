/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
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
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.elasticsearch.testutil.junit.SkipBelowElasticsearch50;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
@Category(SkipBelowElasticsearch50.class)
public class Elasticsearch5JavaTimeIT {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Sample.class );

	@Rule
	public final TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void testLocalDate() throws Exception {
		LocalDate date = LocalDate.of( 2012, Month.DECEMBER, 30 );
		Sample sample = new Sample( 1L, "LocalDate example" );
		sample.localDate = date;

		assertThatFieldIsFormatted( sample, "localDate", "2012-12-30" );
	}

	@Test
	public void testLocalTimeMilliseconds() throws Exception {
		LocalTime time = LocalTime.of( 13, 15, 55, 7_000_000 );

		Sample sample = new Sample( 1L, "LocalTime example" );
		sample.localTime = time;

		assertThatFieldIsFormatted( sample, "localTime", "13:15:55.007", "13:15:55.007" );
	}

	@Test
	public void testLocalTimeNanoseconds() throws Exception {
		LocalTime time = LocalTime.of( 13, 15, 55, 7 );

		Sample sample = new Sample( 1L, "LocalTime example" );
		sample.localTime = time;

		// Elasticsearch only has millisecond-precision, so the "fields" value is missing the nanoseconds
		assertThatFieldIsFormatted( sample, "localTime", "13:15:55.000000007", "13:15:55.000" );
	}

	@Test
	public void testLocalDateTimeMilliseconds() throws Exception {
		LocalDate date = LocalDate.of( 221998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33, 7_000_000 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );

		Sample sample = new Sample( 1L, "LocalDateTime example" );
		sample.localDateTime = dateTime;

		assertThatFieldIsFormatted( sample, "localDateTime", "+221998-02-12T13:05:33.007", "221998-02-12T13:05:33.007" );
	}

	@Test
	public void testLocalDateTimeNanoseconds() throws Exception {
		LocalDate date = LocalDate.of( 221998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33, 7 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );

		Sample sample = new Sample( 1L, "LocalDateTime example" );
		sample.localDateTime = dateTime;

		// Elasticsearch only has millisecond-precision, so the "fields" value is missing the nanoseconds
		assertThatFieldIsFormatted( sample, "localDateTime", "+221998-02-12T13:05:33.000000007", "221998-02-12T13:05:33.000" );
	}

	@Test
	public void testInstant() throws Exception {
		LocalDate date = LocalDate.of( 1998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33, 5 * 1000_000 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );
		Instant instant = dateTime.toInstant( ZoneOffset.UTC );

		Sample sample = new Sample( 1L, "Instant example" );
		sample.instant = instant;

		assertThatFieldIsFormatted( sample, "instant", "1998-02-12T13:05:33.005Z" );
	}

	@Test
	public void testOffsetDateTimeMilliseconds() throws Exception {
		OffsetDateTime value = OffsetDateTime.of(
				221998, Month.FEBRUARY.getValue(), 12,
				13, 05, 33, 7_000_000,
				ZoneOffset.of( "+01:00" )
				);

		Sample sample = new Sample( 1L, "OffsetDateTime example" );
		sample.offsetDateTime = value;

		// The "fields" attribute only ever contains UTC date/times
		assertThatFieldIsFormatted( sample, "offsetDateTime", "+221998-02-12T13:05:33.007+01:00", "221998-02-12T12:05:33.007Z" );
	}

	@Test
	public void testOffsetDateTimeNanoseconds() throws Exception {
		OffsetDateTime value = OffsetDateTime.of(
				221998, Month.FEBRUARY.getValue(), 12,
				13, 05, 33, 7,
				ZoneOffset.of( "+01:00" )
				);

		Sample sample = new Sample( 1L, "OffsetDateTime example" );
		sample.offsetDateTime = value;

		// Elasticsearch only has millisecond-precision, so the "fields" value is missing the nanoseconds
		// Also, the "fields" attribute only ever contains UTC date/times
		assertThatFieldIsFormatted( sample, "offsetDateTime", "+221998-02-12T13:05:33.000000007+01:00", "221998-02-12T12:05:33.000Z" );
	}

	@Test
	public void testOffsetTimeMilliseconds() throws Exception {
		OffsetTime value = OffsetTime.of(
				13, 05, 33, 7_000_000,
				ZoneOffset.of( "+01:00" )
				);

		Sample sample = new Sample( 1L, "OffsetTime example" );
		sample.offsetTime = value;

		// The "fields" attribute only ever contains UTC date/times
		assertThatFieldIsFormatted( sample, "offsetTime", "13:05:33.007+01:00", "12:05:33.007Z" );
	}

	@Test
	public void testOffsetTimeNanoseconds() throws Exception {
		OffsetTime value = OffsetTime.of(
				13, 05, 33, 7,
				ZoneOffset.of( "+01:00" )
				);

		Sample sample = new Sample( 1L, "OffsetTime example" );
		sample.offsetTime = value;

		// Elasticsearch only has millisecond-precision, so the "fields" value is missing the nanoseconds
		// Also, the "fields" attribute only ever contains UTC date/times
		assertThatFieldIsFormatted( sample, "offsetTime", "13:05:33.000000007+01:00", "12:05:33.000Z" );
	}

	@Test
	public void testZonedDateTimeMilliseconds() throws Exception {
		// CET DST rolls back at 2011-10-30 2:59:59 (+02) to 2011-10-30 2:00:00 (+01)
		// Credit: user leonbloy at http://stackoverflow.com/a/18794412/6692043
		LocalDateTime localDateTime = LocalDateTime.of( 2011, 10, 30, 2, 50, 0, 7_000_000 );

		ZonedDateTime value = localDateTime.atZone( ZoneId.of( "CET" ) ).withLaterOffsetAtOverlap();

		Sample sample = new Sample( 1L, "ZonedDateTime example" );
		sample.zonedDateTime = value;

		// The "fields" attribute only ever contains UTC date/times
		assertThatFieldIsFormatted( sample, "zonedDateTime", "2011-10-30T02:50:00.007+01:00[CET]", "2011-10-30T01:50:00.007+00:00[UTC]" );
	}

	@Test
	public void testZonedDateTimeNanoseconds() throws Exception {
		// CET DST rolls back at 2011-10-30 2:59:59 (+02) to 2011-10-30 2:00:00 (+01)
		// Credit: user leonbloy at http://stackoverflow.com/a/18794412/6692043
		LocalDateTime localDateTime = LocalDateTime.of( 2011, 10, 30, 2, 50, 0, 7 );

		ZonedDateTime value = localDateTime.atZone( ZoneId.of( "CET" ) ).withLaterOffsetAtOverlap();

		Sample sample = new Sample( 1L, "ZonedDateTime example" );
		sample.zonedDateTime = value;

		// Elasticsearch only has millisecond-precision, so the "fields" value is missing the nanoseconds
		// Also, the "fields" attribute only ever contains UTC date/times
		assertThatFieldIsFormatted( sample, "zonedDateTime", "2011-10-30T02:50:00.000000007+01:00[CET]", "2011-10-30T01:50:00.000+00:00[UTC]" );
	}

	@Test
	public void testYear() throws Exception {
		/* Elasticsearch only accepts years in the range [-292275054,292278993]
		 */
		Year value = Year.of( 292278993 );

		Sample sample = new Sample( 1L, "Year example" );
		sample.year = value;

		assertThatFieldIsFormatted( sample, "year", "+292278993", "292278993" );
	}

	@Test
	public void testYearMonth() throws Exception {
		YearMonth value = YearMonth.of( 124, 12 );

		Sample sample = new Sample( 1L, "YearMonth example" );
		sample.yearMonth = value;

		assertThatFieldIsFormatted( sample, "yearMonth", "0124-12" );
	}

	@Test
	public void testMonthDay() throws Exception {
		MonthDay value = MonthDay.of( 12, 1 );

		Sample sample = new Sample( 1L, "MonthDay example" );
		sample.monthDay = value;

		assertThatFieldIsFormatted( sample, "monthDay", "--12-01" );
	}

	private void assertThatFieldIsFormatted(Sample sample, String field, String expectedSourceAndFieldValue) throws IOException {
		assertThatFieldIsFormatted( sample, field, expectedSourceAndFieldValue, expectedSourceAndFieldValue );

	}

	private void assertThatFieldIsFormatted(Sample sample, String fieldName, String expectedSourceValue, String expectedFieldValue) throws IOException {
		helper.add( sample, sample.id );

		String documentId = String.valueOf( sample.id );
		JsonObject source = elasticsearchClient.type( Sample.class ).document( documentId ).getSource();
		JsonElement storedField = elasticsearchClient.type( Sample.class ).document( documentId ).getStoredField( fieldName );

		assertEquals( "Unexpected '_source' value", expectedSourceValue,
				source.get( fieldName ).getAsString() );
		assertEquals( "Unexpected 'fields' value", expectedFieldValue,
				storedField.getAsJsonArray().get( 0 ).getAsString() );
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
