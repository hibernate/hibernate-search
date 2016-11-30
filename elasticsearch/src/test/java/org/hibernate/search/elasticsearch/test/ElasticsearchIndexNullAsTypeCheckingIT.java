/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.time.Duration;
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
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link ElasticsearchIndexManager}'s indexNullAs type checking.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexNullAsTypeCheckingIT extends SearchInitializationTestBase {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void indexNullAs_invalid_integer() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Integer" );

		init( IntegerFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class IntegerFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "12.1")
		Integer myField;
	}

	@Test
	public void indexNullAs_invalid_long() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Long" );

		init( LongFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class LongFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "12.1")
		Long myField;
	}

	@Test
	public void indexNullAs_invalid_float() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Float" );

		init( FloatFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class FloatFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "foo")
		Float myField;
	}

	@Test
	public void indexNullAs_invalid_double() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Double" );

		init( DoubleFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class DoubleFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "foo")
		Double myField;
	}

	@Test
	public void indexNullAs_invalid_boolean() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Boolean" );

		init( BooleanFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class BooleanFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "foo")
		boolean myField;
	}

	@Test
	public void indexNullAs_invalid_date() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Date" );
		thrown.expectMessage( "ISO-8601" );

		init( DateFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class DateFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "01/01/2013") // Expected format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ssZ)
		Date myField;
	}

	@Test
	public void indexNullAs_invalid_calendar() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Calendar" );
		thrown.expectMessage( "ISO-8601" );

		init( CalendarFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class CalendarFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "01/01/2013") // Expected format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ssZ)
		Calendar myField;
	}

	@Test
	public void indexNullAs_invalid_duration() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Long" ); // Durations are indexed as longs

		init( DurationFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class DurationFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "PT13M") // Expected format is a number of nanoseconds
		Duration myField;
	}

	@Test
	public void indexNullAs_invalid_instant() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Instant" );
		thrown.expectMessage( "ISO-8601" );

		init( InstantFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class InstantFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "01/01/2013") // Expected format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ssZ)
		Instant myField;
	}

	@Test
	public void indexNullAs_invalid_localDateTime() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "LocalDateTime" );
		thrown.expectMessage( "ISO-8601" );

		init( LocalDateTimeFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class LocalDateTimeFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "2013-12-01T18:25:00+01:00") // Expected format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ss)
		LocalDateTime myField;
	}

	@Test
	public void indexNullAs_invalid_localDate() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "LocalDate" );
		thrown.expectMessage( "ISO-8601" );

		init( LocalDateFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class LocalDateFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "2013-12-01T18:25:00") // Expected format is ISO-8601 (yyyy-MM-dd)
		LocalDate myField;
	}

	@Test
	public void indexNullAs_invalid_localTime() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "LocalTime" );
		thrown.expectMessage( "ISO-8601" );

		init( LocalTimeFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class LocalTimeFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "18:25:00+01:00") // Expected format is ISO-8601 (HH:mm:ss)
		LocalTime myField;
	}

	@Test
	public void indexNullAs_invalid_offsetDateTime() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "OffsetDateTime" );
		thrown.expectMessage( "ISO-8601" );

		init( OffsetDateTimeFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class OffsetDateTimeFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "2013-12-01T18:25:00[UTC]") // Expected format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ssZ)
		OffsetDateTime myField;
	}

	@Test
	public void indexNullAs_invalid_offsetTime() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "OffsetTime" );
		thrown.expectMessage( "ISO-8601" );

		init( OffsetTimeFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class OffsetTimeFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "18:25:00[UTC]") // Expected format is ISO-8601 (HH:mm:ssZ)
		OffsetTime myField;
	}

	@Test
	public void indexNullAs_invalid_zonedDateTime() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "ZonedDateTime" );
		thrown.expectMessage( "ISO-8601" );

		init( ZonedDateTimeFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class ZonedDateTimeFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "2013-02-15T18:25:00+01:00") // Expected format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ss[ZZZ])
		ZonedDateTime myField;
	}

	@Test
	public void indexNullAs_invalid_year() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "Year" );
		thrown.expectMessage( "ISO-8601" );

		init( YearFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class YearFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "2013-02") // Expected format is ISO-8601 (for instance 2014)
		Year myField;
	}

	@Test
	public void indexNullAs_invalid_yearMonth() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "YearMonth" );
		thrown.expectMessage( "ISO-8601" );

		init( YearMonthFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class YearMonthFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "02/2013") // Expected format is ISO-8601 (yyyy-MM)
		YearMonth myField;
	}

	@Test
	public void indexNullAs_invalid_monthDay() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000325" );
		thrown.expectMessage( "myField" );
		thrown.expectMessage( "MonthDay" );
		thrown.expectMessage( "ISO-8601" );

		init( MonthDayFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class MonthDayFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "12-31") // Expected format is ISO-8601 (--MM-dd)
		MonthDay myField;
	}

}
