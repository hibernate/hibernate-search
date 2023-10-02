/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalTime;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class LocalTimePropertyTypeDescriptor extends PropertyTypeDescriptor<LocalTime, LocalTime> {

	public static final LocalTimePropertyTypeDescriptor INSTANCE = new LocalTimePropertyTypeDescriptor();

	private LocalTimePropertyTypeDescriptor() {
		super( LocalTime.class );
	}

	@Override
	protected PropertyValues<LocalTime, LocalTime> createValues() {
		return PropertyValues.<LocalTime>passThroughBuilder()
				.add( LocalTime.MIN, "00:00:00" )
				.add( LocalTime.of( 7, 0, 0 ), "07:00:00" )
				.add( LocalTime.of( 12, 0, 0 ), "12:00:00" )
				.add( LocalTime.of( 12, 0, 1 ), "12:00:01" )
				.add( LocalTime.MAX, "23:59:59.999999999" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<LocalTime> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<LocalTime>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(LocalTime identifier) {
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
	public DefaultValueBridgeExpectations<LocalTime, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<LocalTime, LocalTime>() {

			@Override
			public Class<LocalTime> getIndexFieldJavaType() {
				return LocalTime.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, LocalTime propertyValue) {
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
			public LocalTime getNullAsValueBridge1() {
				return LocalTime.MIDNIGHT;
			}

			@Override
			public LocalTime getNullAsValueBridge2() {
				return LocalTime.of( 12, 30, 15 );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		LocalTime id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		LocalTime id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		LocalTime myProperty;
		@GenericField(indexNullAs = "00:00:00")
		LocalTime indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		LocalTime myProperty;
		@GenericField(indexNullAs = "12:30:15")
		LocalTime indexNullAsProperty;
	}
}
