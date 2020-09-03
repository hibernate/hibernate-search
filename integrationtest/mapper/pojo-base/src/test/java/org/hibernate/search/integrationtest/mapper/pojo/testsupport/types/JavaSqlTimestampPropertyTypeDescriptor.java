/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaSqlTimestampPropertyTypeDescriptor extends PropertyTypeDescriptor<Timestamp> {

	JavaSqlTimestampPropertyTypeDescriptor() {
		super( Timestamp.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Timestamp>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Timestamp, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Timestamp, Instant>() {
			@Override
			public Class<Timestamp> getProjectionType() {
				return Timestamp.class;
			}

			@Override
			public Class<Instant> getIndexFieldJavaType() {
				return Instant.class;
			}

			@Override
			public List<Timestamp> getEntityPropertyValues() {
				return Arrays.asList(
						date( Long.MIN_VALUE ),
						date( 1970, 1, 1, 0, 0, 0, 0 ),
						date( 1970, 1, 9, 13, 28, 59, 0 ),
						date( 2017, 11, 6, 19, 19, 0, 540 ),
						date( Long.MAX_VALUE ),

						// A february 29th on a leap year
						date( 2000, 2, 29, 12, 0, 0, 0 ),
						// A february 29th on a leap year in the Julian calendar (java.util), but not the Gregorian calendar (java.time)
						date( 1500, 2, 29, 12, 0, 0, 0 )
				);
			}

			@Override
			public List<Instant> getDocumentFieldValues() {
				return Arrays.asList(
						Instant.ofEpochMilli( Long.MIN_VALUE ),
						Instant.parse( "1970-01-01T00:00:00.00Z" ),
						Instant.parse( "1970-01-09T13:28:59.00Z" ),
						Instant.parse( "2017-11-06T19:19:00.54Z" ),
						Instant.ofEpochMilli( Long.MAX_VALUE ),

						Instant.parse( "2000-02-29T12:00:00.0Z" ),
						// The Julian calendar is 10 days late at this point
						// See https://en.wikipedia.org/wiki/Proleptic_Gregorian_calendar#Difference_between_Julian_and_proleptic_Gregorian_calendar_dates
						Instant.parse( "1500-03-10T12:00:00.0Z" )
				);
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Timestamp propertyValue) {
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
				return Instant.parse( "2017-11-06T19:19:00.54Z" );
			}
		} );
	}

	private static Timestamp date(long epochMilli) {
		return new Timestamp( epochMilli );
	}

	static Timestamp date(int year, int month, int day, int hour, int minute, int second, int millisecond) {
		Calendar calendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ), Locale.ROOT );
		calendar.clear();
		calendar.set( year, month - 1, day, hour, minute, second );
		calendar.set( Calendar.MILLISECOND, millisecond );
		return new Timestamp( calendar.getTimeInMillis() );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Timestamp myProperty;
		@GenericField(indexNullAs = "1970-01-01T00:00:00.00Z")
		Timestamp indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Timestamp myProperty;
		@GenericField(indexNullAs = "2017-11-06T19:19:00.54Z")
		Timestamp indexNullAsProperty;
	}
}
