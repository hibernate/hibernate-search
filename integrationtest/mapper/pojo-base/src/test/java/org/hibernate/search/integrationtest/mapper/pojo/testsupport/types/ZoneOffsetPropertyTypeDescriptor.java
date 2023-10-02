/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.ZoneOffset;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class ZoneOffsetPropertyTypeDescriptor extends PropertyTypeDescriptor<ZoneOffset, Integer> {

	public static final ZoneOffsetPropertyTypeDescriptor INSTANCE = new ZoneOffsetPropertyTypeDescriptor();

	private ZoneOffsetPropertyTypeDescriptor() {
		super( ZoneOffset.class );
	}

	@Override
	protected PropertyValues<ZoneOffset, Integer> createValues() {
		return PropertyValues.<ZoneOffset, Integer>builder()
				.add( ZoneOffset.MIN, -18 * 3600, "-18:00" )
				.add( ZoneOffset.ofHours( -1 ), -1 * 3600, "-01:00" )
				.add( ZoneOffset.UTC, 0, "Z" )
				.add( ZoneOffset.ofHours( 1 ), 1 * 3600, "+01:00" )
				.add( ZoneOffset.ofHours( 7 ), 7 * 3600, "+07:00" )
				.add( ZoneOffset.MAX, 18 * 3600, "+18:00" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<ZoneOffset> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<ZoneOffset>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(ZoneOffset identifier) {
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
	public DefaultValueBridgeExpectations<ZoneOffset, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<ZoneOffset, Integer>() {

			@Override
			public Class<Integer> getIndexFieldJavaType() {
				return Integer.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, ZoneOffset propertyValue) {
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
			public Integer getNullAsValueBridge1() {
				return ZoneOffset.UTC.getTotalSeconds();
			}

			@Override
			public Integer getNullAsValueBridge2() {
				return ZoneOffset.ofHoursMinutesSeconds( -8, -30, -52 ).getTotalSeconds();
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		ZoneOffset id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		ZoneOffset id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		ZoneOffset myProperty;
		@GenericField(indexNullAs = "+00:00")
		ZoneOffset indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		ZoneOffset myProperty;
		@GenericField(indexNullAs = "-08:30:52")
		ZoneOffset indexNullAsProperty;
	}
}
