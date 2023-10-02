/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class ZonedDateTimePropertyTypeDescriptor extends PropertyTypeDescriptor<ZonedDateTime, ZonedDateTime> {

	public static final ZonedDateTimePropertyTypeDescriptor INSTANCE = new ZonedDateTimePropertyTypeDescriptor();

	private ZonedDateTimePropertyTypeDescriptor() {
		super( ZonedDateTime.class );
	}

	@Override
	protected PropertyValues<ZonedDateTime, ZonedDateTime> createValues() {
		return PropertyValues.<ZonedDateTime>passThroughBuilder()
				.add( ZonedDateTime.of( LocalDateTime.MIN, ZoneId.of( "Europe/Paris" ) ),
						"-999999999-01-01T00:00:00+00:09:21[Europe/Paris]" )
				.add( ZonedDateTime.of( LocalDateTime.of( 1970, Month.JANUARY, 1, 7, 0, 0 ),
						ZoneId.of( "Europe/Paris" ) ), "1970-01-01T07:00:00+01:00[Europe/Paris]" )
				.add( ZonedDateTime.of(
						LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 0, 0 ),
						ZoneId.of( "Europe/Paris" ) ), "1999-01-01T07:00:00+01:00[Europe/Paris]" )
				.add( ZonedDateTime.of(
						LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 0, 0 ),
						ZoneId.of( "America/Chicago" ) ), "1999-01-01T07:00:00-06:00[America/Chicago]" )
				.add( ZonedDateTime.of( LocalDateTime.MAX, ZoneId.of( "Europe/Paris" ) ),
						"+999999999-12-31T23:59:59.999999999+01:00[Europe/Paris]" )
				// Two date/times that could be ambiguous due to a daylight saving time switch
				.add( LocalDateTime.parse( "2011-10-30T02:50:00.00" ).atZone( ZoneId.of( "CET" ) ).withEarlierOffsetAtOverlap(),
						"2011-10-30T02:50:00+02:00[CET]" )
				.add( LocalDateTime.parse( "2011-10-30T02:50:00.00" ).atZone( ZoneId.of( "CET" ) ).withLaterOffsetAtOverlap(),
						"2011-10-30T02:50:00+01:00[CET]" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<ZonedDateTime> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<ZonedDateTime>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(ZonedDateTime identifier) {
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
	public DefaultValueBridgeExpectations<ZonedDateTime, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<ZonedDateTime, ZonedDateTime>() {

			@Override
			public Class<ZonedDateTime> getIndexFieldJavaType() {
				return ZonedDateTime.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, ZonedDateTime propertyValue) {
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
				return ZonedDateTime.of( LocalDateTime.of( 1999, Month.MAY, 31, 9, 30, 10 ), ZoneId.of( "America/Chicago" ) );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		ZonedDateTime id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		ZonedDateTime id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		ZonedDateTime myProperty;
		@GenericField(indexNullAs = "1970-01-01T00:00:00Z[GMT]")
		ZonedDateTime indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		ZonedDateTime myProperty;
		@GenericField(indexNullAs = "1999-05-31T09:30:10-05:00[America/Chicago]")
		ZonedDateTime indexNullAsProperty;
	}
}
