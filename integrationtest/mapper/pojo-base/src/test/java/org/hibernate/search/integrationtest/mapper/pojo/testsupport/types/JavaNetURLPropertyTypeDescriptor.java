/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import static org.assertj.core.api.Assertions.fail;

import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaNetURLPropertyTypeDescriptor extends PropertyTypeDescriptor<URL, String> {

	public static final JavaNetURLPropertyTypeDescriptor INSTANCE = new JavaNetURLPropertyTypeDescriptor();

	private JavaNetURLPropertyTypeDescriptor() {
		super( URL.class );
	}

	@Override
	protected PropertyValues<URL, String> createValues() {
		PropertyValues.StringBasedBuilder<URL> builder = PropertyValues.stringBasedBuilder();
		for ( String string : new String[] {
				"https://www.google.com",
				"https://twitter.com/Hibernate/status/1093118957194801152",
				"https://twitter.com/Hibernate/status/1092803949533507584",
				"https://access.redhat.com/",
				"https://access.redhat.com/products",
				"https://access.redhat.com/products/red-hat-fuse/",
				"https://access.redhat.com/products/red-hat-openshift-container-platform/",
				// No normalization is expected
				"https://www.google.com/./foo/bar/../bar"
		} ) {
			builder.add( url( string ), string );
		}
		return builder.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<URL> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<URL>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(URL identifier) {
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
	public DefaultValueBridgeExpectations<URL, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<URL, String>() {

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, URL propertyValue) {
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
				return "https://hibernate.org";
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		URL id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		URL id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		URL myProperty;
		@GenericField(indexNullAs = "https://www.redhat.com")
		URL indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		URL myProperty;
		@GenericField(indexNullAs = "https://hibernate.org")
		URL indexNullAsProperty;
	}

	private static URL url(String spec) {
		try {
			return new URL( spec );
		}
		catch (MalformedURLException e) {
			fail( "Wrong test URL: " + e.getMessage() );
			return null;
		}
	}
}
