/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Instant;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class InstantPropertyTypeDescriptor extends PropertyTypeDescriptor<Instant, Instant> {

	public static final InstantPropertyTypeDescriptor INSTANCE = new InstantPropertyTypeDescriptor();

	private InstantPropertyTypeDescriptor() {
		super( Instant.class );
	}

	@Override
	protected PropertyValues<Instant, Instant> createValues() {
		return PropertyValues.<Instant>passThroughBuilder()
				.add( Instant.MIN, "-1000000000-01-01T00:00:00Z" )
				.add( Instant.parse( "1970-01-01T00:00:00.00Z" ), "1970-01-01T00:00:00Z" )
				.add( Instant.parse( "1970-01-09T13:28:59.00Z" ), "1970-01-09T13:28:59Z" )
				.add( Instant.parse( "2017-11-06T19:19:00.54Z" ), "2017-11-06T19:19:00.540Z" )
				.add( Instant.MAX, "+1000000000-12-31T23:59:59.999999999Z" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Instant> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Instant>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Instant identifier) {
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
	public DefaultValueBridgeExpectations<Instant, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Instant, Instant>() {

			@Override
			public Class<Instant> getIndexFieldJavaType() {
				return Instant.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Instant propertyValue) {
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
			public Instant getNullAsValueBridge1() {
				return Instant.parse( "1970-01-01T00:00:00.00Z" );
			}

			@Override
			public Instant getNullAsValueBridge2() {
				return Instant.parse( "2017-11-06T19:19:03.54Z" );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Instant id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Instant id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Instant myProperty;
		@GenericField(indexNullAs = "1970-01-01T00:00:00.00Z")
		Instant indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Instant myProperty;
		@GenericField(indexNullAs = "2017-11-06T19:19:03.54Z")
		Instant indexNullAsProperty;
	}
}
