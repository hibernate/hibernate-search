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

public class PrimitiveCharacterPropertyTypeDescriptor extends PropertyTypeDescriptor<Character> {

	PrimitiveCharacterPropertyTypeDescriptor() {
		super( char.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Character>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Character, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Character, String>() {
			@Override
			public Class<Character> getProjectionType() {
				return Character.class;
			}

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public List<Character> getEntityPropertyValues() {
				return Arrays.asList( Character.MIN_VALUE, '7', 'A', 'a', 'f', Character.MAX_VALUE );
			}

			@Override
			public List<String> getDocumentFieldValues() {
				return Arrays.asList( Character.toString( Character.MIN_VALUE ), "7", "A", "a", "f", Character.toString( Character.MAX_VALUE ) );
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Character propertyValue) {
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
			public String getNullAsValueBridge1() {
				return "0";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "F";
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		int id;
		char myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public char getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		int id;
		char myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public char getMyProperty() {
			return myProperty;
		}
	}
}
