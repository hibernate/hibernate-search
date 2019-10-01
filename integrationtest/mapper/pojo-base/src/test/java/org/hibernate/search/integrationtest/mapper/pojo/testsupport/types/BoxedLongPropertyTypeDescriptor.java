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

public class BoxedLongPropertyTypeDescriptor extends PropertyTypeDescriptor<Long> {

	BoxedLongPropertyTypeDescriptor() {
		super( Long.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Long>> getDefaultIdentifierBridgeExpectations() {
		return Optional.of( new DefaultIdentifierBridgeExpectations<Long>() {
			@Override
			public List<Long> getEntityIdentifierValues() {
				return Arrays.asList( Long.MIN_VALUE, -1L, 0L, 1L, 42L, Long.MAX_VALUE );
			}

			@Override
			public List<String> getDocumentIdentifierValues() {
				return Arrays.asList(
						String.valueOf( Long.MIN_VALUE ), "-1", "0", "1", "42", String.valueOf( Long.MAX_VALUE )
				);
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Long identifier) {
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
	public Optional<DefaultValueBridgeExpectations<Long, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Long, Long>() {
			@Override
			public Class<Long> getProjectionType() {
				return Long.class;
			}

			@Override
			public Class<Long> getIndexFieldJavaType() {
				return Long.class;
			}

			@Override
			public List<Long> getEntityPropertyValues() {
				return Arrays.asList( Long.MIN_VALUE, -1L, 0L, 1L, 42L, Long.MAX_VALUE );
			}

			@Override
			public List<Long> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Long propertyValue) {
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
			public Long getNullAsValueBridge1() {
				return 0L;
			}

			@Override
			public Long getNullAsValueBridge2() {
				return 739739L;
			}
		} );
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_INDEX_NAME)
	public static class TypeWithIdentifierBridge1 {
		Long id;
		@DocumentId
		public Long getId() {
			return id;
		}
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_INDEX_NAME)
	public static class TypeWithIdentifierBridge2 {
		Long id;
		@DocumentId
		public Long getId() {
			return id;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		Long myProperty;
		Long indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Long getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "0")
		public Long getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Long myProperty;
		Long indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Long getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "739739")
		public Long getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}
