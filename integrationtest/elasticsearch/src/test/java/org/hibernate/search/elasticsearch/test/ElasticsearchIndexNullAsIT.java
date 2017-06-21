/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

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
import java.util.List;

import javax.persistence.Column;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.elasticsearch.ElasticsearchProjectionConstants;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.test.util.JsonHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2415")
public class ElasticsearchIndexNullAsIT {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Sample.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void testString() throws Exception {
		String string = "foo";

		Sample sample = new Sample( 1L, "String example" );
		sample.string = string;

		assertNullFieldIndexingAndQuerying( "string", string, sample );
	}

	@Test
	public void testInteger() throws Exception {
		Integer integerValue = 42;

		Sample sample = new Sample( 1L, "Integer example" );
		sample.integerField = integerValue;

		assertNullFieldIndexingAndQuerying( "integerField", integerValue, sample );
	}

	@Test
	public void testLong() throws Exception {
		Long longValue = 42L;

		Sample sample = new Sample( 1L, "Long example" );
		sample.longField = longValue;

		assertNullFieldIndexingAndQuerying( "longField", longValue, sample );
	}

	@Test
	public void testFloat() throws Exception {
		Float floatValue = 42.0f;

		Sample sample = new Sample( 1L, "Float example" );
		sample.floatField = floatValue;

		assertNullFieldIndexingAndQuerying( "floatField", floatValue, sample );
	}

	@Test
	public void testDouble() throws Exception {
		Double doubleValue = 42.0d;

		Sample sample = new Sample( 1L, "Double example" );
		sample.doubleField = doubleValue;

		assertNullFieldIndexingAndQuerying( "doubleField", doubleValue, sample );
	}

	@Test
	public void testBoolean() throws Exception {
		Boolean booleanValue = false;

		Sample sample = new Sample( 1L, "Boolean example" );
		sample.booleanField = booleanValue;

		assertNullFieldIndexingAndQuerying( "booleanField", booleanValue, sample );
	}

	@Test
	public void testLocalDate() throws Exception {
		LocalDate date = LocalDate.of( 2012, Month.DECEMBER, 30 );

		Sample sample = new Sample( 1L, "LocalDate example" );
		sample.localDate = date;

		assertNullFieldIndexingAndQuerying( "localDate", date, sample );
	}

	@Test
	public void testLocalTime() throws Exception {
		LocalTime time = LocalTime.of( 13, 15, 55, 7 );

		Sample sample = new Sample( 1L, "LocalTime example" );
		sample.localTime = time;

		assertNullFieldIndexingAndQuerying( "localTime", time, sample );
	}

	@Test
	public void testLocalDateTime() throws Exception {
		LocalDate date = LocalDate.of( 1998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );

		Sample sample = new Sample( 1L, "LocalDateTime example" );
		sample.localDateTime = dateTime;

		assertNullFieldIndexingAndQuerying( "localDateTime", dateTime, sample );
	}

	@Test
	public void testInstant() throws Exception {
		LocalDate date = LocalDate.of( 1998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33, 5 * 1_000_000 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );
		Instant instant = dateTime.toInstant( ZoneOffset.UTC );

		Sample sample = new Sample( 1L, "Instant example" );
		sample.instant = instant;

		assertNullFieldIndexingAndQuerying( "instant", instant, sample );
	}

	@Test
	public void testDuration() throws Exception {
		Duration value = Duration.ofNanos( Long.MAX_VALUE );

		Sample sample = new Sample( 1L, "Duration example" );
		sample.duration = value;

		assertNullFieldIndexingAndQuerying( "duration", value, sample );
	}

	@Test
	public void testPeriod() throws Exception {
		Period value = Period.ZERO;

		Sample sample = new Sample( 1L, "Period example" );
		sample.period = value;

		assertNullFieldIndexingAndQuerying( "period", value, sample );
	}

	@Test
	public void testZoneOffset() throws Exception {
		ZoneOffset value = ZoneOffset.of( "+01:00" );

		Sample sample = new Sample( 1L, "zoneOffset example" );
		sample.zoneOffset = value;

		assertNullFieldIndexingAndQuerying( "zoneOffset", value, sample );
	}

	@Test
	public void testZoneId() throws Exception {
		ZoneId value = ZoneId.of( "GMT" );

		Sample sample = new Sample( 1L, "ZoneId example" );
		sample.zoneId = value;

		assertNullFieldIndexingAndQuerying( "zoneId", value, sample );
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

		assertNullFieldIndexingAndQuerying( "offsetDateTime", value, sample );
	}

	@Test
	public void testOffsetTime() throws Exception {
		OffsetTime value = OffsetTime.of(
				13, 05, 33, 7,
				ZoneOffset.of( "+01:00" )
				);

		Sample sample = new Sample( 1L, "OffsetTime example" );
		sample.offsetTime = value;

		assertNullFieldIndexingAndQuerying( "offsetTime", value, sample );
	}

	@Test
	public void testYear() throws Exception {
		/* Elasticsearch only accepts years in the range [-292275054,292278993]
		 */
		Year value = Year.of( 292278993 );

		Sample sample = new Sample( 1L, "Year example" );
		sample.year = value;

		assertNullFieldIndexingAndQuerying( "year", value, sample );
	}

	@Test
	public void testYearMonth() throws Exception {
		YearMonth value = YearMonth.of( 124, 12 );

		Sample sample = new Sample( 1L, "YearMonth example" );
		sample.yearMonth = value;

		assertNullFieldIndexingAndQuerying( "yearMonth", value, sample );
	}

	@Test
	public void testMonthDay() throws Exception {
		MonthDay value = MonthDay.of( 12, 1 );

		Sample sample = new Sample( 1L, "MonthDay example" );
		sample.monthDay = value;

		assertNullFieldIndexingAndQuerying( "monthDay", value, sample );
	}

	private void assertNullFieldIndexingAndQuerying(String field, Object expectedValue, Sample sampleWithValue) {
		Sample sampleWithoutValue = new Sample( 2L, sampleWithValue.description + " - without value" );

		ExtendedSearchIntegrator factory = sfHolder.getSearchFactory();

		helper.add()
				.push( sampleWithValue, sampleWithValue.id )
				.push( sampleWithoutValue, sampleWithoutValue.id )
				.execute();

		Query query = helper.queryBuilder( Sample.class ).keyword().onField( field ).ignoreAnalyzer().matching( expectedValue ).createQuery();
		List<EntityInfo> result = factory.createHSQuery( query, Sample.class )
				.projection( ElasticsearchProjectionConstants.ID )
				.queryEntityInfos();
		assertThat( result ).as( "Both documents (with field '" + field + "' index as null and with the same field"
				+ " equal to indexNullAs) should be found when querying the indexNullAs value" )
				.hasSize( 2 );

		result = factory.createHSQuery( query, Sample.class )
				.projection( ElasticsearchProjectionConstants.ID, field, ElasticsearchProjectionConstants.SOURCE )
				.queryEntityInfos();
		for ( EntityInfo entityInfo : result ) {
			Object[] projection = entityInfo.getProjection();
			if ( projection[0].equals( 1L ) ) {
				assertThat( projection[1] ).as( "Document with field '" + field + "' non-null should have a non-null"
						+ " projection on this field" )
						.isEqualTo( expectedValue );

				JsonElement json = new Gson().fromJson( (String) projection[2], JsonElement.class );
				JsonElement propertyValue = json.getAsJsonObject().get( field );
				assertThat( propertyValue ).as( "Document with field '" + field + "' non-null should have a value for"
						+ " this field in their source" )
						.isNotNull();
				assertThat( propertyValue ).as( "Document with field '" + field + "' non-null should have a non-null"
						+ " value for this field in their source" )
						.isNotEqualTo( JsonNull.INSTANCE );
			}
			else {
				assertThat( projection[1] ).as( "Document with field '" + field + "' indexed as null should have a null"
						+ " projection on this field" )
						.isNull();
				// Document with a field indexed as null should have null for this field in their source
				JsonHelper.assertJsonEqualsIgnoringUnknownFields( "{'" + field + "': null}", (String) projection[2] );
			}
		}

		query = helper.queryBuilder( Sample.class ).keyword().onField( field ).ignoreAnalyzer().matching( null ).createQuery();
		result = factory.createHSQuery( query, Sample.class )
				.projection( ElasticsearchProjectionConstants.ID )
				.queryEntityInfos();
		assertThat( result ).as( "Both documents (with field '" + field + "' index as null and with the same field equal"
				+ " to indexNullAs) should be found when querying the null value" )
				.hasSize( 2 );
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

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "foo")
		private String string;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "42")
		private Integer integerField;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "42")
		private Long longField;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "42.0")
		private Float floatField;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "42.0")
		private Double doubleField;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "false")
		private Boolean booleanField;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "2012-12-30")
		private LocalDate localDate;

		@Column(name = "LOCAL_TIME") // localTime is a special keywork for some db
		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "13:15:55.000000007")
		private LocalTime localTime;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "1998-02-12T13:05:33")
		private LocalDateTime localDateTime;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "1998-02-12T13:05:33.005+00:00[UTC]")
		private Instant instant;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "9223372036854775807")
		private Duration duration;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "+0000000000+0000000000+0000000000")
		private Period period;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "+01:00")
		private ZoneOffset zoneOffset;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "GMT")
		private ZoneId zoneId;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "+221998-02-12T13:05:33.000000007+01:00")
		private OffsetDateTime offsetDateTime;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "13:05:33.000000007+01:00")
		private OffsetTime offsetTime;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "+292278993")
		private Year year;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "0124-12")
		private YearMonth yearMonth;

		@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "--12-01")
		private MonthDay monthDay;
	}
}
