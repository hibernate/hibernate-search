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

public class BoxedDoublePropertyTypeDescriptor extends PropertyTypeDescriptor<Double, Double> {

	public static final BoxedDoublePropertyTypeDescriptor INSTANCE = new BoxedDoublePropertyTypeDescriptor();

	private BoxedDoublePropertyTypeDescriptor() {
		super( Double.class );
	}

	@Override
	protected PropertyValues<Double, Double> createValues() {
		return PropertyValues.<Double>passThroughBuilder()
				.add( Double.MIN_VALUE, "4.9E-324" )
				.add( -1.0, "-1.0" )
				.add( 0.0, "0.0" )
				.add( 1.0, "1.0" )
				.add( 42.0, "42.0" )
				.add( Double.MAX_VALUE, "1.7976931348623157E308" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Double> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Double>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Double identifier) {
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
	public DefaultValueBridgeExpectations<Double, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Double, Double>() {

			@Override
			public Class<Double> getIndexFieldJavaType() {
				return Double.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Double propertyValue) {
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
			public Double getNullAsValueBridge1() {
				return 0.0;
			}

			@Override
			public Double getNullAsValueBridge2() {
				return 739739.739;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Double id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Double id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Double myProperty;
		@GenericField(indexNullAs = "0")
		Double indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Double myProperty;
		Double indexNullAsProperty;

		@DocumentId
		public int getId() {
			return id;
		}

		@GenericField
		public Double getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "739739.739")
		public Double getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}
