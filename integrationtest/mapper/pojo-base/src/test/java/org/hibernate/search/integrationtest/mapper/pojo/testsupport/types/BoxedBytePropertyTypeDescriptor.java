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

public class BoxedBytePropertyTypeDescriptor extends PropertyTypeDescriptor<Byte> {

	BoxedBytePropertyTypeDescriptor() {
		super( Byte.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Byte>> getDefaultIdentifierBridgeExpectations() {
		return Optional.of( new DefaultIdentifierBridgeExpectations<Byte>() {
			@Override
			public List<Byte> getEntityIdentifierValues() {
				return Arrays.asList( Byte.MIN_VALUE, (byte)-1, (byte)0, (byte)1, (byte)42, Byte.MAX_VALUE );
			}

			@Override
			public List<String> getDocumentIdentifierValues() {
				return Arrays.asList(
						String.valueOf( Byte.MIN_VALUE ), "-1", "0", "1", "42", String.valueOf( Byte.MAX_VALUE )
				);
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Byte identifier) {
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
	public Optional<DefaultValueBridgeExpectations<Byte, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Byte, Byte>() {
			@Override
			public Class<Byte> getProjectionType() {
				return Byte.class;
			}

			@Override
			public Class<Byte> getIndexFieldJavaType() {
				return Byte.class;
			}

			@Override
			public List<Byte> getEntityPropertyValues() {
				return Arrays.asList( Byte.MIN_VALUE, (byte)-1, (byte)0, (byte)1, (byte)42, Byte.MAX_VALUE );
			}

			@Override
			public List<Byte> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Byte propertyValue) {
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
			public Byte getNullAsValueBridge1() {
				return (byte)0;
			}

			@Override
			public Byte getNullAsValueBridge2() {
				return (byte)-64;
			}
		} );
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Byte id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Byte id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Byte myProperty;
		@GenericField(indexNullAs = "0")
		Byte indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Byte myProperty;
		Byte indexNullAsProperty;

		@DocumentId
		public int getId() {
			return id;
		}

		@GenericField
		public Byte getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "-64")
		public Byte getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}
