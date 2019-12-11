/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaNetURIPropertyTypeDescriptor extends PropertyTypeDescriptor<URI> {

	private static final String[] STRING_URLS = {
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
	};

	JavaNetURIPropertyTypeDescriptor() {
		super( URI.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<URI>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<URI, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<URI, String>() {
			@Override
			public Class<URI> getProjectionType() {
				return URI.class;
			}

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public List<URI> getEntityPropertyValues() {
				return Arrays.stream( STRING_URLS ).map( s -> URI.create( s ) ).collect( Collectors.toList() );
			}

			@Override
			public List<String> getDocumentFieldValues() {
				return Arrays.asList( STRING_URLS );
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
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		URI myProperty;
		URI indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public URI getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "https://www.redhat.com")
		public URI getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		URI myProperty;
		URI indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public URI getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "file:///~calendar")
		public URI getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}
