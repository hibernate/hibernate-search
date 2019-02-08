/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Instant;
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
						calendar( "1930-01-01T00:00:00.00Z", TimeZone.getTimeZone( "GMT+18:00" ) ),
						calendar( "1970-01-01T00:00:00.00Z", TimeZone.getTimeZone( "Europe/Paris" ) ),
						calendar( "1970-01-09T13:28:59.00Z", TimeZone.getTimeZone( "Europe/Paris" ) ),
						calendar( "2017-11-06T19:19:00.54Z", TimeZone.getTimeZone( "Europe/Paris" ) ),
						calendar( "2017-11-06T19:19:00.54Z", TimeZone.getTimeZone( "America/Chicago" ) ),
						calendar( Long.MAX_VALUE, TimeZone.getTimeZone( "Europe/Paris" ) ),
						calendar( Long.MAX_VALUE, TimeZone.getTimeZone( "GMT-18:00" ) )
				);
				return calendars;
			}

			@Override
			public List<ZonedDateTime> getDocumentFieldValues() {
				List<ZonedDateTime> zonedDateTimes = Arrays.asList(
						Instant.parse( "1930-01-01T00:00:00.00Z" ).atZone( ZoneId.of( "GMT+18:00" ) ),
						Instant.parse( "1970-01-01T00:00:00.00Z" ).atZone( ZoneId.of( "Europe/Paris" ) ),
						Instant.parse( "1970-01-09T13:28:59.00Z" ).atZone( ZoneId.of( "Europe/Paris" ) ),
						Instant.parse( "2017-11-06T19:19:00.54Z" ).atZone( ZoneId.of( "Europe/Paris" ) ),
						Instant.parse( "2017-11-06T19:19:00.54Z" ).atZone( ZoneId.of( "America/Chicago" ) ),
						Instant.ofEpochMilli( Long.MAX_VALUE ).atZone( ZoneId.of( "Europe/Paris" ) ),
						Instant.ofEpochMilli( Long.MAX_VALUE ).atZone( ZoneId.of( "GMT-18:00" ) )
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

	private static Calendar calendar(String toParse, TimeZone timeZone) {
		return calendar( Instant.parse( toParse ).toEpochMilli(), timeZone );
	}

	private static Calendar calendar(long epochMilli, TimeZone timeZone) {
		// Even though we generally do not want our tests to be locale-sensitive,
		// here we indeed want to use the default Locale,
		// because that's the locale used when creating a new calendar in the bridge.
		// We expect this test to work regardless of the default Locale.
		Calendar calendar = Calendar.getInstance( timeZone, Locale.getDefault() );
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
