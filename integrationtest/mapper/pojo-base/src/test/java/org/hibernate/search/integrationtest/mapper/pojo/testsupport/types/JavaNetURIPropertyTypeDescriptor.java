/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.net.URI;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaNetURIPropertyTypeDescriptor extends PropertyTypeDescriptor<URI, String> {

	public static final JavaNetURIPropertyTypeDescriptor INSTANCE = new JavaNetURIPropertyTypeDescriptor();

	private JavaNetURIPropertyTypeDescriptor() {
		super( URI.class );
	}

	@Override
	protected PropertyValues<URI, String> createValues() {
		PropertyValues.Builder<URI, String> builder = PropertyValues.builder();
		for ( String string : new String[] {
				"https://www.google.com",
				"https://twitter.com/Hibernate/status/1093118957194801152",
				"https://twitter.com/Hibernate/status/1092803949533507584",
				"https://access.redhat.com/",
				"https://access.redhat.com/products",
				"https://access.redhat.com/products/red-hat-fuse/",
				"https://access.redhat.com/products/red-hat-openshift-container-platform/",
				"mailto:java-net@java.sun.com",
				"urn:isbn:096139210x",
				"file:///~calendar",
				// No normalization is expected
				"https://www.google.com/./foo/bar/../bar"
		} ) {
			builder.add( URI.create( string ), string, string );
		}
		return builder.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<URI> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<URI>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(URI identifier) {
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
	public DefaultValueBridgeExpectations<URI, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<URI, String>() {

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, URI propertyValue) {
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
				return "https://www.redhat.com";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "file:///~calendar";
			}

			@Override
			public String getUnparsableNullAsValue() {
				return "http://www.wrong.uri.com?param1=0 param7=0";
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		URI id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		URI id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		URI myProperty;
		@GenericField(indexNullAs = "https://www.redhat.com")
		URI indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		URI myProperty;
		@GenericField(indexNullAs = "file:///~calendar")
		URI indexNullAsProperty;
	}
}
