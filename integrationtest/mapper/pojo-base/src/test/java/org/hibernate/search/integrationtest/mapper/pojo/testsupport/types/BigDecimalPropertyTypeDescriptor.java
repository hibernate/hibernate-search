/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class BigDecimalPropertyTypeDescriptor extends PropertyTypeDescriptor<BigDecimal> {

	BigDecimalPropertyTypeDescriptor() {
		super( BigDecimal.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<BigDecimal>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<BigDecimal, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<BigDecimal, BigDecimal>() {
			@Override
			public Class<BigDecimal> getProjectionType() {
				return BigDecimal.class;
			}

			@Override
			public Class<BigDecimal> getIndexFieldJavaType() {
				return BigDecimal.class;
			}

			@Override
			public List<BigDecimal> getEntityPropertyValues() {
				return Arrays.asList( BigDecimal.valueOf( -100000.0 ), BigDecimal.valueOf( -1.0 ), BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN,
						BigDecimal.valueOf( 100000.0 ), BigDecimal.valueOf( 42571524, 231254 ) );
			}

			@Override
			public List<BigDecimal> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, BigDecimal propertyValue) {
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
			public BigDecimal getNullAsValueBridge1() {
				return BigDecimal.ZERO;
			}

			@Override
			public BigDecimal getNullAsValueBridge2() {
				return BigDecimal.valueOf( 42571524, 231254 );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		BigDecimal myProperty;
		BigDecimal indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public BigDecimal getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "0")
		public BigDecimal getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		BigDecimal myProperty;
		BigDecimal indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public BigDecimal getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "4.2571524E-231247")
		public BigDecimal getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}
