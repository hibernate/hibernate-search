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

public class BoxedFloatPropertyTypeDescriptor extends PropertyTypeDescriptor<Float, Float> {

	public static final BoxedFloatPropertyTypeDescriptor INSTANCE = new BoxedFloatPropertyTypeDescriptor();

	private BoxedFloatPropertyTypeDescriptor() {
		super( Float.class );
	}

	@Override
	protected PropertyValues<Float, Float> createValues() {
		return PropertyValues.<Float>passThroughBuilder()
				.add( Float.MIN_VALUE, "1.4E-45" )
				.add( -1.0f, "-1.0" )
				.add( 0.0f, "0.0" )
				.add( 1.0f, "1.0" )
				.add( 42.0f, "42.0" )
				.add( Float.MAX_VALUE, "3.4028235E38" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Float> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Float>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Float identifier) {
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
	public DefaultValueBridgeExpectations<Float, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Float, Float>() {

			@Override
			public Class<Float> getIndexFieldJavaType() {
				return Float.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Float propertyValue) {
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
			public Float getNullAsValueBridge1() {
				return 0.0f;
			}

			@Override
			public Float getNullAsValueBridge2() {
				return 37.33379f;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Float id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Float id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Float myProperty;
		@GenericField(indexNullAs = "0")
		Float indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Float myProperty;
		@GenericField(indexNullAs = "37.33379")
		Float indexNullAsProperty;
	}
}
