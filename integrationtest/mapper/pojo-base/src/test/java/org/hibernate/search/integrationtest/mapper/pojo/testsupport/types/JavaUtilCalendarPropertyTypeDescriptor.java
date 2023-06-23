/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaUtilCalendarPropertyTypeDescriptor extends PropertyTypeDescriptor<Calendar, ZonedDateTime> {

	public static final JavaUtilCalendarPropertyTypeDescriptor INSTANCE = new JavaUtilCalendarPropertyTypeDescriptor();

	private JavaUtilCalendarPropertyTypeDescriptor() {
		super( Calendar.class );
	}

	@Override
	protected PropertyValues<Calendar, ZonedDateTime> createValues() {
		return PropertyValues.<Calendar, ZonedDateTime>builder()
				.add( calendar( 1930, 1, 1, 0, 0, 0, 0, "GMT+18:00" ),
						zonedDateTime( "1930-01-01T00:00:00.00", "GMT+18:00" ),
						"1930-01-01T00:00:00+18:00[GMT+18:00]" )
				.add( calendar( 1970, 1, 1, 0, 0, 0, 0, "Europe/Paris" ),
						zonedDateTime( "1970-01-01T00:00:00.00", "Europe/Paris" ),
						"1970-01-01T00:00:00+01:00[Europe/Paris]" )
				.add( calendar( 1970, 1, 9, 13, 28, 59, 0, "Europe/Paris" ),
						zonedDateTime( "1970-01-09T13:28:59.00", "Europe/Paris" ),
						"1970-01-09T13:28:59+01:00[Europe/Paris]" )
				.add( calendar( 2017, 11, 6, 19, 19, 0, 540, "Europe/Paris" ),
						zonedDateTime( "2017-11-06T19:19:00.54", "Europe/Paris" ),
						"2017-11-06T19:19:00.54+01:00[Europe/Paris]" )
				.add( calendar( 2017, 11, 6, 19, 19, 0, 540, "America/Chicago" ),
						zonedDateTime( "2017-11-06T19:19:00.54", "America/Chicago" ),
						"2017-11-06T19:19:00.54-06:00[America/Chicago]" )
				.add( calendar( Long.MAX_VALUE, "UTC" ),
						zonedDateTime( Long.MAX_VALUE, "UTC" ),
						"+292278994-08-17T07:12:55.807Z[UTC]" )
				.add( calendar( Long.MAX_VALUE, "GMT-18:00" ),
						zonedDateTime( Long.MAX_VALUE, "GMT-18:00" ),
						"+292278994-08-16T13:12:55.807-18:00[GMT-18:00]" )

				// A february 29th on a leap year
				.add( calendar( 2000, 2, 29, 12, 0, 0, 0, "UTC" ),
						zonedDateTime( "2000-02-29T12:00:00.00", "UTC" ),
						"2000-02-29T12:00:00Z[UTC]" )
				// A february 29th on a leap year in the Julian calendar (java.util), but not the Gregorian calendar (java.time)
				// The Julian calendar is 10 days late at this point
				// See https://en.wikipedia.org/wiki/Proleptic_Gregorian_calendar#Difference_between_Julian_and_proleptic_Gregorian_calendar_dates
				.add( calendar( 1500, 2, 29, 12, 0, 0, 0, "UTC" ),
						zonedDateTime( "1500-03-10T12:00:00.00", "UTC" ),
						"1500-03-10T12:00:00Z[UTC]" )

				// Two date/times that could be ambiguous due to a daylight saving time switch
				.add( calendar(
						LocalDateTime.parse( "2011-10-30T02:50:00.00" ).atZone( ZoneId.of( "CET" ) )
								.withEarlierOffsetAtOverlap().toInstant().toEpochMilli(),
						"CET"
				),
						zonedDateTime( "2011-10-30T02:50:00.00", "CET" ).withEarlierOffsetAtOverlap(),
						"2011-10-30T02:50:00+02:00[CET]" )
				.add( calendar(
						LocalDateTime.parse( "2011-10-30T02:50:00.00" ).atZone( ZoneId.of( "CET" ) )
								.withLaterOffsetAtOverlap().toInstant().toEpochMilli(),
						"CET"
				),
						zonedDateTime( "2011-10-30T02:50:00.00", "CET" ).withLaterOffsetAtOverlap(),
						"2011-10-30T02:50:00+01:00[CET]" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Calendar> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Calendar>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Calendar identifier) {
				TypeWithIdentifierBridge1 instance = new TypeWithIdentifierBridge1();
				instance.id = identifier;
				return instance;
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge2() {
				return TypeWithIdentifierBridge2.class;
			}
		};
	}

	@Override
	public DefaultValueBridgeExpectations<Calendar, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Calendar, ZonedDateTime>() {

			@Override
			public Class<ZonedDateTime> getIndexFieldJavaType() {
				return ZonedDateTime.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Calendar propertyValue) {
				TypeWithValueBridge1 instance = new TypeWithValueBridge1();
				instance.id = identifier;
				instance.myProperty = propertyValue;
				return instance;
			}

			@Override
			public Class<?> getTypeWithValueBridge2() {
				return TypeWithValueBridge2.class;
			}

			@Override
			public ZonedDateTime getNullAsValueBridge1() {
				return ZonedDateTime.of( LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0 ), ZoneId.of( "GMT" ) );
			}

			@Override
			public ZonedDateTime getNullAsValueBridge2() {
				return ZonedDateTime.of( LocalDateTime.of( 2017, Month.NOVEMBER, 5, 19, 0, 54 ), ZoneId.of( "Europe/Paris" ) );
			}
		};
	}

	private static ZonedDateTime zonedDateTime(long epochMilli, String zoneId) {
		return Instant.ofEpochMilli( epochMilli ).atZone( ZoneId.of( zoneId ) );
	}

	private static ZonedDateTime zonedDateTime(String toParse, String zoneId) {
		return LocalDateTime.parse( toParse ).atZone( ZoneId.of( zoneId ) );
	}

	private static Calendar calendar(long epochMilli, String timeZone) {
		// See below, we want to use the default locale
		Calendar calendar = GregorianCalendar.getInstance( TimeZone.getTimeZone( timeZone ), Locale.getDefault() );
		calendar.setTimeInMillis( epochMilli );
		return calendar;
	}

	private static Calendar calendar(int year, int month, int day, int hour, int minute, int second, int millisecond,
			String timeZone) {
		// Even though we generally do not want our tests to be locale-sensitive,
		// here we indeed want to use the default Locale,
		// because that's the locale used when creating a new calendar in the bridge.
		// We expect this test to work regardless of the default Locale.
		Locale locale = Locale.getDefault();
		Calendar calendar = new GregorianCalendar( TimeZone.getTimeZone( timeZone ), locale );
		calendar.clear();
		calendar.set( year, month - 1, day, hour, minute, second );
		calendar.set( Calendar.MILLISECOND, millisecond );
		return calendar;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Calendar id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Calendar id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Calendar myProperty;
		@GenericField(indexNullAs = "1970-01-01T00:00:00Z[GMT]")
		Calendar indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Calendar myProperty;
		@GenericField(indexNullAs = "2017-11-05T19:00:54+01:00[Europe/Paris]")
		Calendar indexNullAsProperty;
	}
}
