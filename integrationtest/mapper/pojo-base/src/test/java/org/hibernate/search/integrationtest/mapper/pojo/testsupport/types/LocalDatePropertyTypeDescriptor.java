/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDate;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class LocalDatePropertyTypeDescriptor extends PropertyTypeDescriptor<LocalDate, LocalDate> {

	public static final LocalDatePropertyTypeDescriptor INSTANCE = new LocalDatePropertyTypeDescriptor();

	private LocalDatePropertyTypeDescriptor() {
		super( LocalDate.class );
	}

	@Override
	protected PropertyValues<LocalDate, LocalDate> createValues() {
		return PropertyValues.<LocalDate>passThroughBuilder()
				.add( LocalDate.MIN, "-999999999-01-01" )
				.add( LocalDate.parse( "1970-01-01" ), "1970-01-01" )
				.add( LocalDate.parse( "1970-01-09" ), "1970-01-09" )
				.add( LocalDate.parse( "2017-11-06" ), "2017-11-06" )
				.add( LocalDate.MAX, "+999999999-12-31" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<LocalDate> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<LocalDate>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(LocalDate identifier) {
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
	public DefaultValueBridgeExpectations<LocalDate, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<LocalDate, LocalDate>() {

			@Override
			public Class<LocalDate> getIndexFieldJavaType() {
				return LocalDate.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, LocalDate propertyValue) {
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
			public LocalDate getNullAsValueBridge1() {
				return LocalDate.parse( "1970-01-01" );
			}

			@Override
			public LocalDate getNullAsValueBridge2() {
				return LocalDate.parse( "2017-11-06" );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		LocalDate id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		LocalDate id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		LocalDate myProperty;
		@GenericField(indexNullAs = "1970-01-01")
		LocalDate indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		LocalDate myProperty;
		@GenericField(indexNullAs = "2017-11-06")
		LocalDate indexNullAsProperty;
	}
}
