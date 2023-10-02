/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Month;
import java.time.MonthDay;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class MonthDayPropertyTypeDescriptor extends PropertyTypeDescriptor<MonthDay, MonthDay> {

	public static final MonthDayPropertyTypeDescriptor INSTANCE = new MonthDayPropertyTypeDescriptor();

	private MonthDayPropertyTypeDescriptor() {
		super( MonthDay.class );
	}

	@Override
	protected PropertyValues<MonthDay, MonthDay> createValues() {
		return PropertyValues.<MonthDay>passThroughBuilder()
				.add( MonthDay.of( Month.JANUARY, 1 ), "--01-01" )
				.add( MonthDay.of( Month.MARCH, 1 ), "--03-01" )
				.add( MonthDay.of( Month.MARCH, 2 ), "--03-02" )
				.add( MonthDay.of( Month.NOVEMBER, 21 ), "--11-21" )
				.add( MonthDay.of( Month.DECEMBER, 31 ), "--12-31" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<MonthDay> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<MonthDay>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(MonthDay identifier) {
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
	public DefaultValueBridgeExpectations<MonthDay, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<MonthDay, MonthDay>() {

			@Override
			public Class<MonthDay> getIndexFieldJavaType() {
				return MonthDay.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, MonthDay propertyValue) {
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
			public MonthDay getNullAsValueBridge1() {
				return MonthDay.of( Month.JANUARY, 1 );
			}

			@Override
			public MonthDay getNullAsValueBridge2() {
				return MonthDay.of( Month.NOVEMBER, 21 );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		MonthDay id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		MonthDay id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		MonthDay myProperty;
		@GenericField(indexNullAs = "--01-01")
		MonthDay indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		MonthDay myProperty;
		@GenericField(indexNullAs = "--11-21")
		MonthDay indexNullAsProperty;
	}
}
