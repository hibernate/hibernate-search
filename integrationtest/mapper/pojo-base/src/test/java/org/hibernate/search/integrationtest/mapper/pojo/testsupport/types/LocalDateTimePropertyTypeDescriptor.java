/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDateTime;
import java.time.Month;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class LocalDateTimePropertyTypeDescriptor extends PropertyTypeDescriptor<LocalDateTime, LocalDateTime> {

	public static final LocalDateTimePropertyTypeDescriptor INSTANCE = new LocalDateTimePropertyTypeDescriptor();

	private LocalDateTimePropertyTypeDescriptor() {
		super( LocalDateTime.class );
	}

	@Override
	protected PropertyValues<LocalDateTime, LocalDateTime> createValues() {
		return PropertyValues.<LocalDateTime>passThroughBuilder()
				.add( LocalDateTime.MIN, "-999999999-01-01T00:00:00" )
				.add( LocalDateTime.of( 1970, Month.JANUARY, 1, 7, 0, 0 ),
						"1970-01-01T07:00:00" )
				.add( LocalDateTime.of( 1970, Month.JANUARY, 9, 7, 0, 0 ),
						"1970-01-09T07:00:00" )
				.add( LocalDateTime.of( 2017, Month.JANUARY, 9, 7, 0, 0 ),
						"2017-01-09T07:00:00" )
				.add( LocalDateTime.MAX, "+999999999-12-31T23:59:59.999999999" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<LocalDateTime> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<LocalDateTime>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(LocalDateTime identifier) {
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
	public DefaultValueBridgeExpectations<LocalDateTime, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<LocalDateTime, LocalDateTime>() {

			@Override
			public Class<LocalDateTime> getIndexFieldJavaType() {
				return LocalDateTime.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, LocalDateTime propertyValue) {
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
			public LocalDateTime getNullAsValueBridge1() {
				return LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0 );
			}

			@Override
			public LocalDateTime getNullAsValueBridge2() {
				return LocalDateTime.of( 2030, Month.NOVEMBER, 21, 15, 15, 15 );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		LocalDateTime id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		LocalDateTime id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		LocalDateTime myProperty;
		@GenericField(indexNullAs = "1970-01-01T00:00:00")
		LocalDateTime indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		LocalDateTime myProperty;
		@GenericField(indexNullAs = "2030-11-21T15:15:15")
		LocalDateTime indexNullAsProperty;
	}
}
