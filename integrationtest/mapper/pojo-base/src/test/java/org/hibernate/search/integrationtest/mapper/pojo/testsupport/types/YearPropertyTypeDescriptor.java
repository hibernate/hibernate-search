/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Year;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class YearPropertyTypeDescriptor extends PropertyTypeDescriptor<Year, Year> {

	public static final YearPropertyTypeDescriptor INSTANCE = new YearPropertyTypeDescriptor();

	private YearPropertyTypeDescriptor() {
		super( Year.class );
	}

	@Override
	protected PropertyValues<Year, Year> createValues() {
		return PropertyValues.<Year>passThroughBuilder()
				.add( Year.of( Year.MIN_VALUE ), "-999999999" )
				.add( Year.of( -1 ), "-0001" )
				.add( Year.of( 0 ), "0000" )
				.add( Year.of( 1 ), "0001" )
				.add( Year.of( 42 ), "0042" )
				.add( Year.of( Year.MAX_VALUE ), "+999999999" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Year> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Year>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Year identifier) {
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
	public DefaultValueBridgeExpectations<Year, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Year, Year>() {

			@Override
			public Class<Year> getIndexFieldJavaType() {
				return Year.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Year propertyValue) {
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
			public Year getNullAsValueBridge1() {
				return Year.of( 0 );
			}

			@Override
			public Year getNullAsValueBridge2() {
				return Year.of( 2020 );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Year id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Year id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Year myProperty;
		@GenericField(indexNullAs = "0000")
		Year indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Year myProperty;
		@GenericField(indexNullAs = "2020")
		Year indexNullAsProperty;
	}
}
