/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.ZoneId;
import java.time.ZoneOffset;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class ZoneIdPropertyTypeDescriptor extends PropertyTypeDescriptor<ZoneId, String> {

	public static final ZoneIdPropertyTypeDescriptor INSTANCE = new ZoneIdPropertyTypeDescriptor();

	private ZoneIdPropertyTypeDescriptor() {
		super( ZoneId.class );
	}

	@Override
	protected PropertyValues<ZoneId, String> createValues() {
		return PropertyValues.<ZoneId>stringBasedBuilder()
				.add( ZoneOffset.MIN, "-18:00" )
				.add( ZoneId.of( "America/Los_Angeles" ), "America/Los_Angeles" )
				.add( ZoneOffset.UTC, "Z" )
				.add( ZoneId.of( "Europe/Paris" ), "Europe/Paris" )
				.add( ZoneOffset.ofHours( 7 ), "+07:00" )
				.add( ZoneOffset.MAX, "+18:00" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<ZoneId> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<ZoneId>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(ZoneId identifier) {
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
	public DefaultValueBridgeExpectations<ZoneId, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<ZoneId, String>() {

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, ZoneId propertyValue) {
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
				return ZoneOffset.UTC.getId();
			}

			@Override
			public String getNullAsValueBridge2() {
				return ZoneId.of( "Europe/Paris" ).getId();
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		ZoneId id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		ZoneId id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		ZoneId myProperty;
		@GenericField(indexNullAs = "Z")
		ZoneId indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		ZoneId myProperty;
		@GenericField(indexNullAs = "Europe/Paris")
		ZoneId indexNullAsProperty;
	}
}
