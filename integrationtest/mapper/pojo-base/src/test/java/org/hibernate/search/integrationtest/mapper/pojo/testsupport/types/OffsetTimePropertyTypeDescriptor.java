/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class OffsetTimePropertyTypeDescriptor extends PropertyTypeDescriptor<OffsetTime, OffsetTime> {

	public static final OffsetTimePropertyTypeDescriptor INSTANCE = new OffsetTimePropertyTypeDescriptor();

	private OffsetTimePropertyTypeDescriptor() {
		super( OffsetTime.class );
	}

	@Override
	protected PropertyValues<OffsetTime, OffsetTime> createValues() {
		return PropertyValues.<OffsetTime>passThroughBuilder()
				.add( OffsetTime.MIN, "00:00:00+18:00" )
				.add( LocalTime.of( 7, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
						"07:00:00+01:00" )
				.add( LocalTime.of( 12, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
						"12:00:00+01:00" )
				.add( LocalTime.of( 12, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
						"12:00:01+01:00" )
				.add( LocalTime.of( 12, 0, 1 ).atOffset( ZoneOffset.ofHours( -6 ) ),
						"12:00:01-06:00" )
				.add( OffsetTime.MAX, "23:59:59.999999999-18:00" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<OffsetTime> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<OffsetTime>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(OffsetTime identifier) {
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
	public DefaultValueBridgeExpectations<OffsetTime, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<OffsetTime, OffsetTime>() {

			@Override
			public Class<OffsetTime> getIndexFieldJavaType() {
				return OffsetTime.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, OffsetTime propertyValue) {
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
			public OffsetTime getNullAsValueBridge1() {
				return LocalTime.MIDNIGHT.atOffset( ZoneOffset.UTC );
			}

			@Override
			public OffsetTime getNullAsValueBridge2() {
				return LocalTime.of( 12, 30, 55 ).atOffset( ZoneOffset.ofHours( -3 ) );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		OffsetTime id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		OffsetTime id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		OffsetTime myProperty;
		@GenericField(indexNullAs = "00:00:00Z")
		OffsetTime indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		OffsetTime myProperty;
		@GenericField(indexNullAs = "12:30:55-03:00")
		OffsetTime indexNullAsProperty;
	}
}
