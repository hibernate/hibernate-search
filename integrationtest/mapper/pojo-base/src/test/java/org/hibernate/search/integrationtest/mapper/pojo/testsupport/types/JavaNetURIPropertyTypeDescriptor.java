/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.net.URI;
import java.util.Optional;

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
				"file:///~calendar"
		} ) {
			builder.add( URI.create( string ), string );
		}
		return builder.build();
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<URI>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
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
