/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class StringPropertyTypeDescriptor extends PropertyTypeDescriptor<String> {

	StringPropertyTypeDescriptor() {
		super( String.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<String>> getDefaultIdentifierBridgeExpectations() {
		return Optional.of( new DefaultIdentifierBridgeExpectations<String>() {
			@Override
			public List<String> getEntityIdentifierValues() {
				return Arrays.asList( "", "a", "AaaA", "Some words", "\000", "http://foo", "yop@yopmail.com" );
			}

			@Override
			public List<String> getDocumentIdentifierValues() {
				return getEntityIdentifierValues();
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(String identifier) {
				TypeWithIdentifierBridge1 instance = new TypeWithIdentifierBridge1();
				instance.id = identifier;
				return instance;
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge2() {
				return TypeWithIdentifierBridge2.class;
			}
		} );
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<String, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<String, String>() {
			@Override
			public Class<String> getProjectionType() {
				return String.class;
			}

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public List<String> getEntityPropertyValues() {
				return Arrays.asList( "", "a", "AaaA", "Some words", "\000", "http://foo", "yop@yopmail.com" );
			}

			@Override
			public List<String> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, String propertyValue) {
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
				return "";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "some value";
			}

			@Override
			public String getUnparsableNullAsValue() {
				// Every string is valid
				return null;
			}
		} );
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_INDEX_NAME)
	public static class TypeWithIdentifierBridge1 {
		String id;
		@DocumentId
		public String getId() {
			return id;
		}
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_INDEX_NAME)
	public static class TypeWithIdentifierBridge2 {
		String id;
		@DocumentId
		public String getId() {
			return id;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		String myProperty;
		String indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public String getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "")
		public String getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		String myProperty;
		String indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public String getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "some value")
		public String getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}
