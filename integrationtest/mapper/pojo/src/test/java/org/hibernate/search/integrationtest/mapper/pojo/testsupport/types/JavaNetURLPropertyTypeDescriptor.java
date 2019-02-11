/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaNetURLPropertyTypeDescriptor extends PropertyTypeDescriptor<URL> {

	JavaNetURLPropertyTypeDescriptor() {
		super( URL.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<URL>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<URL, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<URL, URI>() {
			@Override
			public Class<URL> getProjectionType() {
				return URL.class;
			}

			@Override
			public Class<URI> getIndexFieldJavaType() {
				return URI.class;
			}

			@Override
			public List<URL> getEntityPropertyValues() {
				List<URL> urls = Arrays.asList(
						url( "https://www.google.com" ),
						url( "https://twitter.com/Hibernate/status/1093118957194801152" ),
						url( "https://twitter.com/Hibernate/status/1092803949533507584" ),
						url( "https://access.redhat.com/" ),
						url( "https://access.redhat.com/products" ),
						url( "https://access.redhat.com/products/red-hat-fuse/" ),
						url( "https://access.redhat.com/products/red-hat-openshift-container-platform/" )
				);
				return urls;
			}

			@Override
			public List<URI> getDocumentFieldValues() {
				List<URI> uris = Arrays.asList(
						URI.create( "https://www.google.com" ),
						URI.create( "https://twitter.com/Hibernate/status/1093118957194801152" ),
						URI.create( "https://twitter.com/Hibernate/status/1092803949533507584" ),
						URI.create( "https://access.redhat.com/" ),
						URI.create( "https://access.redhat.com/products" ),
						URI.create( "https://access.redhat.com/products/red-hat-fuse/" ),
						URI.create( "https://access.redhat.com/products/red-hat-openshift-container-platform/" )
				);
				return uris;
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
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		URL myProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public URL getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		URL myProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public URL getMyProperty() {
			return myProperty;
		}
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
