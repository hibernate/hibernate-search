/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.math.BigDecimal;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class BigDecimalPropertyTypeDescriptor extends PropertyTypeDescriptor<BigDecimal, BigDecimal> {

	public static final BigDecimalPropertyTypeDescriptor INSTANCE = new BigDecimalPropertyTypeDescriptor();

	private BigDecimalPropertyTypeDescriptor() {
		super( BigDecimal.class );
	}

	@Override
	protected PropertyValues<BigDecimal, BigDecimal> createValues() {
		return PropertyValues.<BigDecimal>passThroughBuilder()
				.add( BigDecimal.valueOf( -100000.0 ) )
				.add( BigDecimal.valueOf( -1.0 ) )
				.add( BigDecimal.ZERO )
				.add( BigDecimal.ONE )
				.add( BigDecimal.TEN )
				.add( BigDecimal.valueOf( 100000.0 ) )
				.add( BigDecimal.valueOf( 42571524, 231254 ) )
				.build();
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<BigDecimal>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public DefaultValueBridgeExpectations<BigDecimal, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<BigDecimal, BigDecimal>() {

			@Override
			public Class<BigDecimal> getIndexFieldJavaType() {
				return BigDecimal.class;
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
		};
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		BigDecimal myProperty;
		@GenericField(indexNullAs = "0")
		BigDecimal indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		BigDecimal myProperty;
		@GenericField(indexNullAs = "4.2571524E-231247")
		BigDecimal indexNullAsProperty;
	}
}
