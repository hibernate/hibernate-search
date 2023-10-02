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

public class BoxedIntegerPropertyTypeDescriptor extends PropertyTypeDescriptor<Integer, Integer> {

	public static final BoxedIntegerPropertyTypeDescriptor INSTANCE = new BoxedIntegerPropertyTypeDescriptor();

	private BoxedIntegerPropertyTypeDescriptor() {
		super( Integer.class );
	}

	@Override
	protected PropertyValues<Integer, Integer> createValues() {
		return PropertyValues.<Integer>passThroughBuilder()
				.add( Integer.MIN_VALUE, String.valueOf( Integer.MIN_VALUE ) )
				.add( -1, "-1" )
				.add( 0, "0" )
				.add( 1, "1" )
				.add( 42, "42" )
				.add( Integer.MAX_VALUE, String.valueOf( Integer.MAX_VALUE ) )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Integer> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Integer>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Integer identifier) {
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
	public DefaultValueBridgeExpectations<Integer, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Integer, Integer>() {

			@Override
			public Class<Integer> getIndexFieldJavaType() {
				return Integer.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Integer propertyValue) {
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
				return 0;
			}

			@Override
			public Integer getNullAsValueBridge2() {
				return 739;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Integer id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Integer id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Integer myProperty;
		@GenericField(indexNullAs = "0")
		Integer indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Integer myProperty;
		@GenericField(indexNullAs = "739")
		Integer indexNullAsProperty;
	}
}
