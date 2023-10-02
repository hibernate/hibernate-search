/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class EnumPropertyTypeDescriptor extends PropertyTypeDescriptor<EnumPropertyTypeDescriptor.MyEnum, String> {

	public static final EnumPropertyTypeDescriptor INSTANCE = new EnumPropertyTypeDescriptor();

	private EnumPropertyTypeDescriptor() {
		super( MyEnum.class );
	}

	@Override
	protected PropertyValues<MyEnum, String> createValues() {
		return PropertyValues.<MyEnum>stringBasedBuilder()
				.add( MyEnum.VALUE1, "VALUE1" )
				.add( MyEnum.VALUE2, "VALUE2" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<MyEnum> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<MyEnum>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(MyEnum identifier) {
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
	public DefaultValueBridgeExpectations<MyEnum, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<MyEnum, String>() {

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, MyEnum propertyValue) {
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
			public String getNullAsValueBridge1() {
				return "VALUE1";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "VALUE2";
			}
		};
	}

	public enum MyEnum {
		VALUE1,
		VALUE2
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		MyEnum id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		MyEnum id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		MyEnum myProperty;
		@GenericField(indexNullAs = "VALUE1")
		MyEnum indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		MyEnum myProperty;
		@GenericField(indexNullAs = "VALUE2")
		MyEnum indexNullAsProperty;
	}
}
