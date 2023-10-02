/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Period;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class PeriodPropertyTypeDescriptor extends PropertyTypeDescriptor<Period, String> {

	public static final PeriodPropertyTypeDescriptor INSTANCE = new PeriodPropertyTypeDescriptor();

	private PeriodPropertyTypeDescriptor() {
		super( Period.class );
	}

	@Override
	protected PropertyValues<Period, String> createValues() {
		return PropertyValues.<Period, String>builder()
				.add( Period.ZERO, "+0000000000+0000000000+0000000000", "P0D" )
				.add( Period.ofDays( 1 ), "+0000000000+0000000000+0000000001", "P1D" )
				.add( Period.ofMonths( 4 ), "+0000000000+0000000004+0000000000", "P4M" )
				.add( Period.ofYears( 2050 ), "+0000002050+0000000000+0000000000", "P2050Y" )
				.add( Period.of( 1900, 12, 21 ), "+0000001900+0000000012+0000000021", "P1900Y12M21D" )
				.add( Period.ofMonths( 24 ), "+0000000000+0000000024+0000000000", "P24M" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Period> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Period>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Period identifier) {
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
	public DefaultValueBridgeExpectations<Period, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Period, String>() {

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Period propertyValue) {
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
			public String getNullAsValueBridge1() {
				return "+0000000000+0000000000+0000000000";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "+0000001900+0000000012+0000000021";
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Period id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Period id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Period myProperty;
		@GenericField(indexNullAs = "P0D")
		Period indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Period myProperty;
		@GenericField(indexNullAs = "P1900Y12M21D")
		Period indexNullAsProperty;
	}
}
