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

public class PrimitiveShortPropertyTypeDescriptor extends PropertyTypeDescriptor<Short, Short> {

	public static final PrimitiveShortPropertyTypeDescriptor INSTANCE = new PrimitiveShortPropertyTypeDescriptor();

	private PrimitiveShortPropertyTypeDescriptor() {
		super( short.class, Short.class );
	}

	@Override
	protected PropertyValues<Short, Short> createValues() {
		return BoxedShortPropertyTypeDescriptor.INSTANCE.values();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Short> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Short>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Short identifier) {
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
	public DefaultValueBridgeExpectations<Short, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Short, Short>() {

			@Override
			public Class<Short> getIndexFieldJavaType() {
				return Short.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Short propertyValue) {
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
			public Short getNullAsValueBridge1() {
				return 0;
			}

			@Override
			public Short getNullAsValueBridge2() {
				return 739;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		short id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		short id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		int id;
		@GenericField
		short myProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		int id;
		@GenericField
		short myProperty;
	}
}
