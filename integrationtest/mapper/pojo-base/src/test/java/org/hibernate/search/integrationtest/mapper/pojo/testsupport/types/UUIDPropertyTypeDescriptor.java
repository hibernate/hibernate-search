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
import java.util.UUID;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class UUIDPropertyTypeDescriptor extends PropertyTypeDescriptor<UUID> {

	UUIDPropertyTypeDescriptor() {
		super( UUID.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<UUID>> getDefaultIdentifierBridgeExpectations() {
		return Optional.of( new DefaultIdentifierBridgeExpectations<UUID>() {
			@Override
			public List<UUID> getEntityIdentifierValues() {
				return getSequence();
			}

			@Override
			public List<String> getDocumentIdentifierValues() {
				return getStrings();
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(UUID identifier) {
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
	public Optional<DefaultValueBridgeExpectations<UUID, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<UUID, String>() {
			@Override
			public Class<UUID> getProjectionType() {
				return UUID.class;
			}

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public List<UUID> getEntityPropertyValues() {
				return getSequence();
			}

			@Override
			public List<String> getDocumentFieldValues() {
				return getStrings();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, UUID propertyValue) {
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
				return "80000000-0000-0000-8000-000000000000";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "8cea97f9-9696-4299-9f05-636a208b6c1f";
			}
		} );
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		UUID id;

		@DocumentId
		public UUID getId() {
			return id;
		}
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		UUID id;

		@DocumentId
		public UUID getId() {
			return id;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		UUID myProperty;
		UUID indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public UUID getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "80000000-0000-0000-8000-000000000000")
		public UUID getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		UUID myProperty;
		UUID indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public UUID getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "8cea97f9-9696-4299-9f05-636a208b6c1f")
		public UUID getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	public static List<UUID> getSequence() {
		return Arrays.asList(
				new UUID( Long.MIN_VALUE, Long.MIN_VALUE ),
				new UUID( Long.MIN_VALUE, -1L ),
				new UUID( Long.MIN_VALUE, 0L ),
				new UUID( Long.MIN_VALUE, 1L ),
				new UUID( Long.MAX_VALUE, Long.MIN_VALUE ),
				new UUID( Long.MAX_VALUE, Long.MAX_VALUE ),
				UUID.fromString( "8cea97f9-9696-4299-9f05-636a208b6c1f" )
		);
	}

	public static List<String> getStrings() {
		return Arrays.asList(
				"80000000-0000-0000-8000-000000000000",
				"80000000-0000-0000-ffff-ffffffffffff",
				"80000000-0000-0000-0000-000000000000",
				"80000000-0000-0000-0000-000000000001",
				"7fffffff-ffff-ffff-8000-000000000000",
				"7fffffff-ffff-ffff-7fff-ffffffffffff",
				"8cea97f9-9696-4299-9f05-636a208b6c1f"
		);
	}
}
