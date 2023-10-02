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

public class BoxedLongPropertyTypeDescriptor extends PropertyTypeDescriptor<Long, Long> {

	public static final BoxedLongPropertyTypeDescriptor INSTANCE = new BoxedLongPropertyTypeDescriptor();

	private BoxedLongPropertyTypeDescriptor() {
		super( Long.class );
	}

	@Override
	protected PropertyValues<Long, Long> createValues() {
		return PropertyValues.<Long>passThroughBuilder()
				.add( Long.MIN_VALUE, String.valueOf( Long.MIN_VALUE ) )
				.add( -1L, "-1" )
				.add( 0L, "0" )
				.add( 1L, "1" )
				.add( 42L, "42" )
				.add( Long.MAX_VALUE, String.valueOf( Long.MAX_VALUE ) )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Long> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Long>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Long identifier) {
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
	public DefaultValueBridgeExpectations<Long, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Long, Long>() {

			@Override
			public Class<Long> getIndexFieldJavaType() {
				return Long.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Long propertyValue) {
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
			public Long getNullAsValueBridge1() {
				return 0L;
			}

			@Override
			public Long getNullAsValueBridge2() {
				return 739739L;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Long id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Long id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Long myProperty;
		@GenericField(indexNullAs = "0")
		Long indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Long myProperty;
		@GenericField(indexNullAs = "739739")
		Long indexNullAsProperty;
	}
}
