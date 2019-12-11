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

public class PrimitiveBooleanPropertyTypeDescriptor extends PropertyTypeDescriptor<Boolean> {

	PrimitiveBooleanPropertyTypeDescriptor() {
		super( boolean.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Boolean>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Boolean, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Boolean, Boolean>() {
			@Override
			public Class<Boolean> getProjectionType() {
				return Boolean.class;
			}

			@Override
			public Class<Boolean> getIndexFieldJavaType() {
				return Boolean.class;
			}

			@Override
			public List<Boolean> getEntityPropertyValues() {
				return Arrays.asList( true, false );
			}

			@Override
			public List<Boolean> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Boolean propertyValue) {
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
			public Boolean getNullAsValueBridge1() {
				return false;
			}

			@Override
			public Boolean getNullAsValueBridge2() {
				return true;
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		int id;
		boolean myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public boolean getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		int id;
		boolean myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public boolean getMyProperty() {
			return myProperty;
		}
	}
}
