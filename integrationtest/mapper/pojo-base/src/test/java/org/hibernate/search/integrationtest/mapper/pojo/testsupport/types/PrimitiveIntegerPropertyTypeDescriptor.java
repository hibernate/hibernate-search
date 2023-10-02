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

public class PrimitiveIntegerPropertyTypeDescriptor extends PropertyTypeDescriptor<Integer, Integer> {

	public static final PrimitiveIntegerPropertyTypeDescriptor INSTANCE = new PrimitiveIntegerPropertyTypeDescriptor();

	private PrimitiveIntegerPropertyTypeDescriptor() {
		super( int.class, Integer.class );
	}

	@Override
	protected PropertyValues<Integer, Integer> createValues() {
		return BoxedIntegerPropertyTypeDescriptor.INSTANCE.values();
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
				// Implicit unboxing
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
				// Implicit unboxing
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
				return 0;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		int id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		int id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		int id;
		@GenericField
		int myProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		int id;
		@GenericField
		int myProperty;
	}
}
