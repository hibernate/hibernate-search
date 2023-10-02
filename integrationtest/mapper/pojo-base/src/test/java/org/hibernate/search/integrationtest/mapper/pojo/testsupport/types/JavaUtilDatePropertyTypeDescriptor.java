/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaUtilDatePropertyTypeDescriptor extends PropertyTypeDescriptor<Date, Instant> {

	public static final JavaUtilDatePropertyTypeDescriptor INSTANCE = new JavaUtilDatePropertyTypeDescriptor();

	private JavaUtilDatePropertyTypeDescriptor() {
		super( Date.class );
	}

	@Override
	protected PropertyValues<Date, Instant> createValues() {
		return PropertyValues.<Date, Instant>builder()
				.add( date( Long.MIN_VALUE ), Instant.ofEpochMilli( Long.MIN_VALUE ),
						"-292275055-05-16T16:47:04.192Z" )
				.add( date( 1970, 1, 1, 0, 0, 0, 0 ),
						Instant.parse( "1970-01-01T00:00:00.00Z" ), "1970-01-01T00:00:00Z" )
				.add( date( 1970, 1, 9, 13, 28, 59, 0 ),
						Instant.parse( "1970-01-09T13:28:59.00Z" ), "1970-01-09T13:28:59Z" )
				.add( date( 2017, 11, 6, 19, 19, 0, 540 ),
						Instant.parse( "2017-11-06T19:19:00.54Z" ), "2017-11-06T19:19:00.540Z" )
				.add( date( Long.MAX_VALUE ), Instant.ofEpochMilli( Long.MAX_VALUE ),
						"+292278994-08-17T07:12:55.807Z" )

				// A february 29th on a leap year
				.add( date( 2000, 2, 29, 12, 0, 0, 0 ),
						Instant.parse( "2000-02-29T12:00:00.0Z" ), "2000-02-29T12:00:00Z" )
				// A february 29th on a leap year in the Julian calendar (java.util), but not the Gregorian calendar (java.time)
				.add( date( 1500, 2, 29, 12, 0, 0, 0 ),
						// The Julian calendar is 10 days late at this point
						// See https://en.wikipedia.org/wiki/Proleptic_Gregorian_calendar#Difference_between_Julian_and_proleptic_Gregorian_calendar_dates
						Instant.parse( "1500-03-10T12:00:00.0Z" ), "1500-03-10T12:00:00Z" )

				// Adding some sql dates. See HSEARCH-3670
				.add( JavaSqlDatePropertyTypeDescriptor.date( 2017, 11, 6, 19, 19, 0, 540 ),
						Instant.parse( "2017-11-06T19:19:00.54Z" ), "2017-11-06T19:19:00.540Z" )
				// A february 29th on a leap year
				.add( JavaSqlDatePropertyTypeDescriptor.date( 2000, 2, 29, 12, 0, 0, 0 ),
						Instant.parse( "2000-02-29T12:00:00.0Z" ), "2000-02-29T12:00:00Z" )
				// A february 29th on a leap year in the Julian calendar (java.util), but not the Gregorian calendar (java.time)
				.add( JavaSqlDatePropertyTypeDescriptor.date( 1500, 2, 29, 12, 0, 0, 0 ),
						// The Julian calendar is 10 days late at this point
						// See https://en.wikipedia.org/wiki/Proleptic_Gregorian_calendar#Difference_between_Julian_and_proleptic_Gregorian_calendar_dates
						Instant.parse( "1500-03-10T12:00:00.0Z" ), "1500-03-10T12:00:00Z" )

				// Adding a sql time. See HSEARCH-3670
				.add( JavaSqlTimePropertyTypeDescriptor.date( 2017, 11, 6, 19, 19, 0, 540 ),
						Instant.parse( "2017-11-06T19:19:00.54Z" ), "2017-11-06T19:19:00.540Z" )

				// Adding a sql timestamp. See HSEARCH-3670
				.add( JavaSqlTimestampPropertyTypeDescriptor.date( 2017, 11, 6, 19, 19, 0, 540 ),
						Instant.parse( "2017-11-06T19:19:00.54Z" ), "2017-11-06T19:19:00.540Z" )
				.build();
	}

	@Override
	public Date toProjectedValue(Date indexedValue) {
		if ( indexedValue.getClass().equals( Date.class ) ) {
			return indexedValue;
		}
		else {
			/*
			 * The bridge always returns a java.util.Date,
			 * even if the original value was a subtype from the java.sql package.
			 */
			return new Date( indexedValue.getTime() );
		}
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Date> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Date>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Date identifier) {
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
	public DefaultValueBridgeExpectations<Date, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Date, Instant>() {

			@Override
			public Class<Instant> getIndexFieldJavaType() {
				return Instant.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Date propertyValue) {
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
			public Instant getNullAsValueBridge1() {
				return Instant.parse( "1970-01-01T00:00:00.00Z" );
			}

			@Override
			public Instant getNullAsValueBridge2() {
				return Instant.parse( "2012-02-29T12:10:01.12Z" );
			}
		};
	}

	private static Date date(long epochMilli) {
		return new Date( epochMilli );
	}

	private static Date date(int year, int month, int day, int hour, int minute, int second, int millisecond) {
		Calendar calendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ), Locale.ROOT );
		calendar.clear();
		calendar.set( year, month - 1, day, hour, minute, second );
		calendar.set( Calendar.MILLISECOND, millisecond );
		return calendar.getTime();
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Date id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Date id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Date myProperty;
		@GenericField(indexNullAs = "1970-01-01T00:00:00.00Z")
		Date indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Date myProperty;
		@GenericField(indexNullAs = "2012-02-29T12:10:01.12Z")
		Date indexNullAsProperty;
	}
}
