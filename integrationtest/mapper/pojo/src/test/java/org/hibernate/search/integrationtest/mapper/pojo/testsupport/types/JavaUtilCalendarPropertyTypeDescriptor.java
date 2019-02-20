/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaUtilCalendarPropertyTypeDescriptor extends PropertyTypeDescriptor<Calendar> {

	JavaUtilCalendarPropertyTypeDescriptor() {
		super( Calendar.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Calendar>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Calendar, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Calendar, ZonedDateTime>() {
			@Override
			public Class<Calendar> getProjectionType() {
				return Calendar.class;
			}

			@Override
			public Class<ZonedDateTime> getIndexFieldJavaType() {
				return ZonedDateTime.class;
			}

			@Override
			public List<Calendar> getEntityPropertyValues() {
				List<Calendar> calendars = Arrays.asList(
						calendar( "1930-01-01T00:00:00.00", "GMT+18:00" ),
						calendar( "1970-01-01T00:00:00.00", "Europe/Paris" ),
						calendar( "1970-01-09T13:28:59.00", "Europe/Paris" ),
						calendar( "2017-11-06T19:19:00.54", "Europe/Paris" ),
						calendar( "2017-11-06T19:19:00.54", "America/Chicago" ),
						calendar( Long.MAX_VALUE, "Europe/Paris" ),
						calendar( Long.MAX_VALUE, "GMT-18:00" ),

						// a february 29th on a leap year
						calendar( "2000-02-29T12:00:00.00", "UTC" ),

						// Two date/times that could be ambiguous due to a daylight saving time switch
						calendar( "2011-10-30T02:50:00.00", "CET" )
				);
				return calendars;
			}

			@Override
			public List<ZonedDateTime> getDocumentFieldValues() {
				List<ZonedDateTime> zonedDateTimes = Arrays.asList(
						zonedDateTime( "1930-01-01T00:00:00.00", "GMT+18:00" ),
						zonedDateTime( "1970-01-01T00:00:00.00", "Europe/Paris" ),
						zonedDateTime( "1970-01-09T13:28:59.00", "Europe/Paris" ),
						zonedDateTime( "2017-11-06T19:19:00.54", "Europe/Paris" ),
						zonedDateTime( "2017-11-06T19:19:00.54", "America/Chicago" ),
						zonedDateTime( Long.MAX_VALUE, "Europe/Paris" ),
						zonedDateTime( Long.MAX_VALUE, "GMT-18:00" ),

						// a february 29th on a leap year
						zonedDateTime( "2000-02-29T12:00:00.00", "UTC" ),

						// Two date/times that could be ambiguous due to a daylight saving time switch
						zonedDateTime( "2011-10-30T02:50:00.00", "CET" )
				);
				return zonedDateTimes;
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
		} );
	}

	private static ZonedDateTime zonedDateTime(long epochMilli, String zoneId) {
		return Instant.ofEpochMilli( epochMilli ).atZone( ZoneId.of( zoneId ) );
	}

	private static ZonedDateTime zonedDateTime(String toParse, String zoneId) {
		return LocalDateTime.parse( toParse ).atZone( ZoneId.of( zoneId ) );
	}

	private static Calendar calendar(String toParse, String timeZone) {
		long epochMilli = LocalDateTime.parse( toParse ).atZone( ZoneId.of( timeZone ) ).toInstant().toEpochMilli();
		return calendar( epochMilli, timeZone );
	}

	private static Calendar calendar(long epochMilli, String timeZone) {
		// Even though we generally do not want our tests to be locale-sensitive,
		// here we indeed want to use the default Locale,
		// because that's the locale used when creating a new calendar in the bridge.
		// We expect this test to work regardless of the default Locale.
		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( timeZone ), Locale.getDefault() );
		calendar.setTimeInMillis( epochMilli );
		return calendar;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		Calendar myProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Calendar getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Calendar myProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Calendar getMyProperty() {
			return myProperty;
		}
	}
}
