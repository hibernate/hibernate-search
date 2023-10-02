/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;

public class PrimitiveByteArrayPropertyTypeDescriptor extends AbstractVectorPropertyTypeDescriptor<byte[], byte[]> {

	public static final PrimitiveByteArrayPropertyTypeDescriptor INSTANCE = new PrimitiveByteArrayPropertyTypeDescriptor();

	private PrimitiveByteArrayPropertyTypeDescriptor() {
		super( byte[].class );
	}

	@Override
	protected PropertyValues<byte[], byte[]> createValues() {
		return PropertyValues.<byte[]>passThroughBuilder()
				.add( new byte[] { 0, 0 }, "[0, 0]" )
				.add( new byte[] { 100, 120 }, "[100, 120]" )
				.add( new byte[] { 41, 12 }, "[41, 12]" )
				.add( new byte[] { -26, 123 }, "[-26, 123]" )
				.add( new byte[] { -14, -53 }, "[-14, -53]" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<byte[]> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(byte[] identifier) {
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
	public DefaultValueBridgeExpectations<byte[], ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<byte[], byte[]>() {

			@Override
			public Class<byte[]> getIndexFieldJavaType() {
				return byte[].class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, byte[] propertyValue) {
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
			public byte[] getNullAsValueBridge1() {
				return new byte[] { 0, 0 };
			}

			@Override
			public byte[] getNullAsValueBridge2() {
				return new byte[] { 100, 120 };
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		byte[] id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		byte[] id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@VectorField(dimension = VECTOR_DIMENSION)
		byte[] myProperty;
		@VectorField(dimension = VECTOR_DIMENSION, indexNullAs = "[0, 0]")
		byte[] indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@VectorField(dimension = VECTOR_DIMENSION)
		byte[] myProperty;
		@VectorField(dimension = VECTOR_DIMENSION, indexNullAs = "100, 120")
		byte[] indexNullAsProperty;
	}
}
