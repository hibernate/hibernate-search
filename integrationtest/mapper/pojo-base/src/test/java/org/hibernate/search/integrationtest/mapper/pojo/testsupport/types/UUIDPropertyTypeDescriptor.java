/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.util.UUID;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class UUIDPropertyTypeDescriptor extends PropertyTypeDescriptor<UUID, String> {

	public static final UUIDPropertyTypeDescriptor INSTANCE = new UUIDPropertyTypeDescriptor();

	private UUIDPropertyTypeDescriptor() {
		super( UUID.class );
	}

	@Override
	protected PropertyValues<UUID, String> createValues() {
		return PropertyValues.<UUID>stringBasedBuilder()
				.add( new UUID( Long.MIN_VALUE, Long.MIN_VALUE ), "80000000-0000-0000-8000-000000000000" )
				.add( new UUID( Long.MIN_VALUE, -1L ), "80000000-0000-0000-ffff-ffffffffffff" )
				.add( new UUID( Long.MIN_VALUE, 0L ), "80000000-0000-0000-0000-000000000000" )
				.add( new UUID( Long.MIN_VALUE, 1L ), "80000000-0000-0000-0000-000000000001" )
				.add( new UUID( Long.MAX_VALUE, Long.MIN_VALUE ), "7fffffff-ffff-ffff-8000-000000000000" )
				.add( new UUID( Long.MAX_VALUE, Long.MAX_VALUE ), "7fffffff-ffff-ffff-7fff-ffffffffffff" )
				.add( UUID.fromString( "8cea97f9-9696-4299-9f05-636a208b6c1f" ),
						"8cea97f9-9696-4299-9f05-636a208b6c1f" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<UUID> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<UUID>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(UUID identifier) {
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
	public DefaultValueBridgeExpectations<UUID, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<UUID, String>() {

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, UUID propertyValue) {
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
				return "80000000-0000-0000-8000-000000000000";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "8cea97f9-9696-4299-9f05-636a208b6c1f";
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		UUID id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		UUID id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		UUID myProperty;
		@GenericField(indexNullAs = "80000000-0000-0000-8000-000000000000")
		UUID indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		UUID myProperty;
		@GenericField(indexNullAs = "8cea97f9-9696-4299-9f05-636a208b6c1f")
		UUID indexNullAsProperty;
	}
}
