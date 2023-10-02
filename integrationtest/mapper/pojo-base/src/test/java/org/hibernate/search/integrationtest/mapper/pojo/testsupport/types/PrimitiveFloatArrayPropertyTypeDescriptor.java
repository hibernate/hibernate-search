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

public class PrimitiveFloatArrayPropertyTypeDescriptor extends AbstractVectorPropertyTypeDescriptor<float[], float[]> {

	public static final PrimitiveFloatArrayPropertyTypeDescriptor INSTANCE = new PrimitiveFloatArrayPropertyTypeDescriptor();

	private PrimitiveFloatArrayPropertyTypeDescriptor() {
		super( float[].class );
	}

	@Override
	protected PropertyValues<float[], float[]> createValues() {
		return PropertyValues.<float[]>passThroughBuilder()
				.add( new float[] { 0.0f, 0.0f }, "[0.0, 0.0]" )
				.add( new float[] { 100.0f, 120.0f }, "[100.0, 120.0]" )
				.add( new float[] { 41.0f, 12.0f }, "[41.0, 12.0]" )
				.add( new float[] { -26.0f, 123.0f }, "[-26.0, 123.0]" )
				.add( new float[] { -14.0f, -53.0f }, "[-14.0, -53.0]" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<float[]> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(float[] identifier) {
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
	public DefaultValueBridgeExpectations<float[], ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<float[], float[]>() {

			@Override
			public Class<float[]> getIndexFieldJavaType() {
				return float[].class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, float[] propertyValue) {
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
			public float[] getNullAsValueBridge1() {
				return new float[] { 0.0f, 0.0f };
			}

			@Override
			public float[] getNullAsValueBridge2() {
				return new float[] { 100.0f, 120.0f };
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		float[] id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		float[] id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@VectorField(dimension = VECTOR_DIMENSION)
		float[] myProperty;
		@VectorField(dimension = VECTOR_DIMENSION, indexNullAs = "0.0, 0.0")
		float[] indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@VectorField(dimension = VECTOR_DIMENSION)
		float[] myProperty;
		@VectorField(dimension = VECTOR_DIMENSION, indexNullAs = "100.0, 120.0")
		float[] indexNullAsProperty;
	}
}
