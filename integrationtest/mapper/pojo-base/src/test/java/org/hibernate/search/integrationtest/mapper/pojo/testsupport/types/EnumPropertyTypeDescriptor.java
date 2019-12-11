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

public class EnumPropertyTypeDescriptor extends PropertyTypeDescriptor<EnumPropertyTypeDescriptor.MyEnum> {

	EnumPropertyTypeDescriptor() {
		super( MyEnum.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<MyEnum>> getDefaultIdentifierBridgeExpectations() {
		return Optional.of( new DefaultIdentifierBridgeExpectations<MyEnum>() {
			@Override
			public List<MyEnum> getEntityIdentifierValues() {
				return Arrays.asList( MyEnum.VALUE1, MyEnum.VALUE2 );
			}

			@Override
			public List<String> getDocumentIdentifierValues() {
				return Arrays.asList( "VALUE1", "VALUE2" );
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(MyEnum identifier) {
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
	public Optional<DefaultValueBridgeExpectations<MyEnum, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<MyEnum, String>() {
			@Override
			public Class<MyEnum> getProjectionType() {
				return MyEnum.class;
			}

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public List<MyEnum> getEntityPropertyValues() {
				return Arrays.asList( MyEnum.VALUE1, MyEnum.VALUE2 );
			}

			@Override
			public List<String> getDocumentFieldValues() {
				return Arrays.asList( "VALUE1", "VALUE2" );
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, MyEnum propertyValue) {
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
				return "VALUE1";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "VALUE2";
			}
		} );
	}

	public enum MyEnum {
		VALUE1,
		VALUE2
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		MyEnum id;
		@DocumentId
		public MyEnum getId() {
			return id;
		}
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		MyEnum id;
		@DocumentId
		public MyEnum getId() {
			return id;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		MyEnum myProperty;
		MyEnum indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public MyEnum getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "VALUE1")
		public MyEnum getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		MyEnum myProperty;
		MyEnum indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public MyEnum getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "VALUE2")
		public MyEnum getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}
