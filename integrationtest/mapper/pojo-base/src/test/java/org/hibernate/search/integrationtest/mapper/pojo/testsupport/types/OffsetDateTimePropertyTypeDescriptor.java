/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class OffsetDateTimePropertyTypeDescriptor extends PropertyTypeDescriptor<OffsetDateTime, OffsetDateTime> {

	public static final OffsetDateTimePropertyTypeDescriptor INSTANCE = new OffsetDateTimePropertyTypeDescriptor();

	private OffsetDateTimePropertyTypeDescriptor() {
		super( OffsetDateTime.class );
	}

	@Override
	protected PropertyValues<OffsetDateTime, OffsetDateTime> createValues() {
		return PropertyValues.<OffsetDateTime>passThroughBuilder()
				.add( OffsetDateTime.of( LocalDateTime.MIN, ZoneOffset.ofHours( 1 ) ),
						"-999999999-01-01T00:00:00+01:00" )
				.add( OffsetDateTime.of( LocalDateTime.of( 1970, Month.JANUARY, 1, 7, 0, 0 ),
						ZoneOffset.ofHours( 1 ) ), "1970-01-01T07:00:00+01:00" )
				.add( OffsetDateTime.of( LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 0, 0 ),
						ZoneOffset.ofHours( 1 ) ), "1999-01-01T07:00:00+01:00" )
				.add( OffsetDateTime.of( LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 0, 0 ),
						ZoneOffset.ofHours( -6 ) ), "1999-01-01T07:00:00-06:00" )
				.add( OffsetDateTime.of( LocalDateTime.MAX, ZoneOffset.ofHours( 1 ) ),
						"+999999999-12-31T23:59:59.999999999+01:00" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<OffsetDateTime> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<OffsetDateTime>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(OffsetDateTime identifier) {
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
	public DefaultValueBridgeExpectations<OffsetDateTime, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<OffsetDateTime, OffsetDateTime>() {

			@Override
			public Class<OffsetDateTime> getIndexFieldJavaType() {
				return OffsetDateTime.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, OffsetDateTime propertyValue) {
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
			public OffsetDateTime getNullAsValueBridge1() {
				return OffsetDateTime.of( LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0 ), ZoneOffset.UTC );
			}

			@Override
			public OffsetDateTime getNullAsValueBridge2() {
				return OffsetDateTime.of( LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 30, 59 ), ZoneOffset.ofHours( -6 ) );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		OffsetDateTime id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		OffsetDateTime id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		OffsetDateTime myProperty;
		@GenericField(indexNullAs = "1970-01-01T00:00:00Z")
		OffsetDateTime indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		OffsetDateTime myProperty;
		@GenericField(indexNullAs = "1999-01-01T07:30:59-06:00")
		OffsetDateTime indexNullAsProperty;
	}
}
