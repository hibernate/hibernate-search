/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.test.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.test.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.test.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class BoxedIntegerPropertyTypeDescriptor extends PropertyTypeDescriptor<Integer> {

	BoxedIntegerPropertyTypeDescriptor() {
		super( Integer.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Integer>> getDefaultIdentifierBridgeExpectations() {
		return Optional.of( new DefaultIdentifierBridgeExpectations<Integer>() {
			@Override
			public List<Integer> getEntityIdentifierValues() {
				return Arrays.asList( Integer.MIN_VALUE, -1, 0, 1, 42, Integer.MAX_VALUE );
			}

			@Override
			public List<String> getDocumentIdentifierValues() {
				return Arrays.asList(
						String.valueOf( Integer.MIN_VALUE ), "-1", "0", "1", "42", String.valueOf( Integer.MAX_VALUE )
				);
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Integer identifier) {
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
	public Optional<DefaultValueBridgeExpectations<Integer, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Integer, Integer>() {
			@Override
			public Class<Integer> getProjectionType() {
				return Integer.class;
			}

			@Override
			public Class<Integer> getIndexFieldJavaType() {
				return Integer.class;
			}

			@Override
			public List<Integer> getEntityPropertyValues() {
				return Arrays.asList( Integer.MIN_VALUE, -1, 0, 1, 42, Integer.MAX_VALUE );
			}

			@Override
			public List<Integer> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Integer propertyValue) {
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

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_INDEX_NAME)
	public static class TypeWithIdentifierBridge1 {
		Integer id;
		@DocumentId
		public Integer getId() {
			return id;
		}
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_INDEX_NAME)
	public static class TypeWithIdentifierBridge2 {
		Integer id;
		@DocumentId
		public Integer getId() {
			return id;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		Integer myProperty;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@GenericField
		public Integer getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Integer myProperty;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@GenericField
		public Integer getMyProperty() {
			return myProperty;
		}
	}
}
