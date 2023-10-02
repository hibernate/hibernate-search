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

public class BoxedCharacterPropertyTypeDescriptor extends PropertyTypeDescriptor<Character, String> {

	public static final BoxedCharacterPropertyTypeDescriptor INSTANCE = new BoxedCharacterPropertyTypeDescriptor();

	private BoxedCharacterPropertyTypeDescriptor() {
		super( Character.class );
	}

	@Override
	protected PropertyValues<Character, String> createValues() {
		return PropertyValues.<Character>stringBasedBuilder()
				.add( Character.MIN_VALUE, String.valueOf( Character.MIN_VALUE ) )
				.add( '7', "7" )
				.add( 'A', "A" )
				.add( 'a', "a" )
				.add( 'f', "f" )
				.add( Character.MAX_VALUE, String.valueOf( Character.MAX_VALUE ) )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Character> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Character>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Character identifier) {
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
	public DefaultValueBridgeExpectations<Character, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Character, String>() {

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Character propertyValue) {
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
				return "7";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "F";
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Character id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Character id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Character myProperty;
		@GenericField(indexNullAs = "7")
		Character indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Character myProperty;
		@GenericField(indexNullAs = "F")
		Character indexNullAsProperty;
	}
}
