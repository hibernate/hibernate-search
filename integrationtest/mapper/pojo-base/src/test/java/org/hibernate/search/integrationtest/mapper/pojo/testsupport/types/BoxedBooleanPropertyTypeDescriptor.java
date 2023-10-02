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

public class BoxedBooleanPropertyTypeDescriptor extends PropertyTypeDescriptor<Boolean, Boolean> {

	public static final BoxedBooleanPropertyTypeDescriptor INSTANCE = new BoxedBooleanPropertyTypeDescriptor();

	private BoxedBooleanPropertyTypeDescriptor() {
		super( Boolean.class );
	}

	@Override
	protected PropertyValues<Boolean, Boolean> createValues() {
		return PropertyValues.<Boolean>passThroughBuilder()
				.add( Boolean.TRUE, "true" )
				.add( Boolean.FALSE, "false" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Boolean> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Boolean>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Boolean identifier) {
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
	public DefaultValueBridgeExpectations<Boolean, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Boolean, Boolean>() {

			@Override
			public Class<Boolean> getIndexFieldJavaType() {
				return Boolean.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Boolean propertyValue) {
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
			public Boolean getNullAsValueBridge1() {
				return Boolean.FALSE;
			}

			@Override
			public Boolean getNullAsValueBridge2() {
				return Boolean.TRUE;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Boolean id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Boolean id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Boolean myProperty;
		@GenericField(indexNullAs = "false")
		Boolean indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Boolean myProperty;
		@GenericField(indexNullAs = "true")
		Boolean indexNullAsProperty;
	}
}
