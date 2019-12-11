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

public class PrimitiveShortPropertyTypeDescriptor extends PropertyTypeDescriptor<Short> {

	PrimitiveShortPropertyTypeDescriptor() {
		super( short.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Short>> getDefaultIdentifierBridgeExpectations() {
		return Optional.of( new DefaultIdentifierBridgeExpectations<Short>() {
			@Override
			public List<Short> getEntityIdentifierValues() {
				return Arrays.asList( Short.MIN_VALUE, (short)-1, (short)0, (short)1, (short)42, Short.MAX_VALUE );
			}

			@Override
			public List<String> getDocumentIdentifierValues() {
				return Arrays.asList(
						String.valueOf( Short.MIN_VALUE ), "-1", "0", "1", "42", String.valueOf( Short.MAX_VALUE )
				);
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Short identifier) {
				TypeWithIdentifierBridge1 instance = new TypeWithIdentifierBridge1();
				// Implicit unboxing
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
	public Optional<DefaultValueBridgeExpectations<Short, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Short, Short>() {
			@Override
			public Class<Short> getProjectionType() {
				return Short.class;
			}

			@Override
			public Class<Short> getIndexFieldJavaType() {
				return Short.class;
			}

			@Override
			public List<Short> getEntityPropertyValues() {
				return Arrays.asList( Short.MIN_VALUE, (short)-1, (short)0, (short)1, (short)42, Short.MAX_VALUE );
			}

			@Override
			public List<Short> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Short propertyValue) {
				TypeWithValueBridge1 instance = new TypeWithValueBridge1();
				instance.id = identifier;
				// Implicit unboxing
				instance.myProperty = propertyValue;
				return instance;
			}

			@Override
			public Class<?> getTypeWithValueBridge2() {
				return TypeWithValueBridge2.class;
			}

			@Override
			public Short getNullAsValueBridge1() {
				return 0;
			}

			@Override
			public Short getNullAsValueBridge2() {
				return 739;
			}
		} );
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		short id;
		@DocumentId
		public short getId() {
			return id;
		}
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		short id;
		@DocumentId
		public short getId() {
			return id;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		int id;
		short myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public short getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		int id;
		short myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public short getMyProperty() {
			return myProperty;
		}
	}
}
