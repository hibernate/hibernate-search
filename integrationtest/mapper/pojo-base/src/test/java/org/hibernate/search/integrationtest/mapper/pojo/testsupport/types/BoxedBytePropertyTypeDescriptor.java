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

public class BoxedBytePropertyTypeDescriptor extends PropertyTypeDescriptor<Byte, Byte> {

	public static final BoxedBytePropertyTypeDescriptor INSTANCE = new BoxedBytePropertyTypeDescriptor();

	private BoxedBytePropertyTypeDescriptor() {
		super( Byte.class );
	}

	@Override
	protected PropertyValues<Byte, Byte> createValues() {
		return PropertyValues.<Byte>passThroughBuilder()
				.add( Byte.MIN_VALUE, String.valueOf( Byte.MIN_VALUE ) )
				.add( (byte) -1, "-1" )
				.add( (byte) 0, "0" )
				.add( (byte) 1, "1" )
				.add( (byte) 42, "42" )
				.add( Byte.MAX_VALUE, String.valueOf( Byte.MAX_VALUE ) )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Byte> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Byte>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Byte identifier) {
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
	public DefaultValueBridgeExpectations<Byte, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Byte, Byte>() {

			@Override
			public Class<Byte> getIndexFieldJavaType() {
				return Byte.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Byte propertyValue) {
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
			public Byte getNullAsValueBridge1() {
				return (byte) 0;
			}

			@Override
			public Byte getNullAsValueBridge2() {
				return (byte) -64;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Byte id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Byte id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Byte myProperty;
		@GenericField(indexNullAs = "0")
		Byte indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Byte myProperty;
		Byte indexNullAsProperty;

		@DocumentId
		public int getId() {
			return id;
		}

		@GenericField
		public Byte getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "-64")
		public Byte getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}
