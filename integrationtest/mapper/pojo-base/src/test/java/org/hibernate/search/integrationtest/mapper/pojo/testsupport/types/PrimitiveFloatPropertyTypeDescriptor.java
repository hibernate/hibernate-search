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

public class PrimitiveFloatPropertyTypeDescriptor extends PropertyTypeDescriptor<Float> {

	PrimitiveFloatPropertyTypeDescriptor() {
		super( float.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Float>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Float, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Float, Float>() {
			@Override
			public Class<Float> getProjectionType() {
				return Float.class;
			}

			@Override
			public Class<Float> getIndexFieldJavaType() {
				return Float.class;
			}

			@Override
			public List<Float> getEntityPropertyValues() {
				return Arrays.asList( Float.MIN_VALUE, -1.0f, 0.0f, 1.0f, 42.0f, Float.MAX_VALUE );
			}

			@Override
			public List<Float> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Float propertyValue) {
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
			public Float getNullAsValueBridge1() {
				return 0.0f;
			}

			@Override
			public Float getNullAsValueBridge2() {
				return 739.937f;
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		int id;
		float myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public float getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		int id;
		float myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public float getMyProperty() {
			return myProperty;
		}
	}
}
