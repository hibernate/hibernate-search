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

public class PrimitiveDoublePropertyTypeDescriptor extends PropertyTypeDescriptor<Double> {

	PrimitiveDoublePropertyTypeDescriptor() {
		super( double.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Double>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Double, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Double, Double>() {
			@Override
			public Class<Double> getProjectionType() {
				return Double.class;
			}

			@Override
			public Class<Double> getIndexFieldJavaType() {
				return Double.class;
			}

			@Override
			public List<Double> getEntityPropertyValues() {
				return Arrays.asList( Double.MIN_VALUE, -1.0, 0.0, 1.0, 42.0, Double.MAX_VALUE );
			}

			@Override
			public List<Double> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Double propertyValue) {
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
			public Double getNullAsValueBridge1() {
				return 0.0;
			}

			@Override
			public Double getNullAsValueBridge2() {
				return 7.739;
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		int id;
		double myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public double getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		int id;
		double myProperty;
		@DocumentId
		public int getId() {
			return id;
		}
		@GenericField
		public double getMyProperty() {
			return myProperty;
		}
	}
}
