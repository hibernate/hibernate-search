/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.math.BigDecimal;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class BigDecimalPropertyTypeDescriptor extends PropertyTypeDescriptor<BigDecimal, BigDecimal> {

	public static final BigDecimalPropertyTypeDescriptor INSTANCE = new BigDecimalPropertyTypeDescriptor();

	private BigDecimalPropertyTypeDescriptor() {
		super( BigDecimal.class );
	}

	@Override
	protected PropertyValues<BigDecimal, BigDecimal> createValues() {
		return PropertyValues.<BigDecimal>passThroughBuilder()
				.add( BigDecimal.valueOf( -100000.0 ), "-100000.0" )
				.add( BigDecimal.valueOf( -1.0 ), "-1.0" )
				.add( BigDecimal.ZERO, "0" )
				.add( BigDecimal.ONE, "1" )
				.add( BigDecimal.TEN, "10" )
				.add( BigDecimal.valueOf( 100000.0 ), "100000.0" )
				.add( BigDecimal.valueOf( 42571524, 231254 ), "4.2571524E-231247" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<BigDecimal> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<BigDecimal>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(BigDecimal identifier) {
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
	public DefaultValueBridgeExpectations<BigDecimal, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<BigDecimal, BigDecimal>() {

			@Override
			public Class<BigDecimal> getIndexFieldJavaType() {
				return BigDecimal.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, BigDecimal propertyValue) {
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
			public BigDecimal getNullAsValueBridge1() {
				return BigDecimal.ZERO;
			}

			@Override
			public BigDecimal getNullAsValueBridge2() {
				return BigDecimal.valueOf( 42571524, 231254 );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		BigDecimal id;

	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		BigDecimal id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		BigDecimal myProperty;
		@GenericField(indexNullAs = "0")
		BigDecimal indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		BigDecimal myProperty;
		@GenericField(indexNullAs = "4.2571524E-231247")
		BigDecimal indexNullAsProperty;
	}
}
