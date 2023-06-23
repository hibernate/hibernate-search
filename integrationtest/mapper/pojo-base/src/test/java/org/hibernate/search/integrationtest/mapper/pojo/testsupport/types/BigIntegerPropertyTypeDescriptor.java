/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.math.BigInteger;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class BigIntegerPropertyTypeDescriptor extends PropertyTypeDescriptor<BigInteger, BigInteger> {

	public static final BigIntegerPropertyTypeDescriptor INSTANCE = new BigIntegerPropertyTypeDescriptor();

	private BigIntegerPropertyTypeDescriptor() {
		super( BigInteger.class );
	}

	@Override
	protected PropertyValues<BigInteger, BigInteger> createValues() {
		return PropertyValues.<BigInteger>passThroughBuilder()
				.add( BigInteger.valueOf( -10000 ), "-10000" )
				.add( BigInteger.valueOf( -1 ), "-1" )
				.add( BigInteger.ZERO, "0" )
				.add( BigInteger.ONE, "1" )
				.add( BigInteger.TEN, "10" )
				.add( BigInteger.valueOf( 10000 ), "10000" )
				.add( BigInteger.valueOf( Long.MAX_VALUE ).multiply( BigInteger.valueOf( 1000 ) ),
						"9223372036854775807000" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<BigInteger> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<BigInteger>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(BigInteger identifier) {
				TypeWithIdentifierBridge1 instance = new TypeWithIdentifierBridge1();
				instance.id = identifier;
				return instance;
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge2() {
				return TypeWithIdentifierBridge2.class;
			}
		};
	}

	@Override
	public DefaultValueBridgeExpectations<BigInteger, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<BigInteger, BigInteger>() {

			@Override
			public Class<BigInteger> getIndexFieldJavaType() {
				return BigInteger.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, BigInteger propertyValue) {
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
			public BigInteger getNullAsValueBridge1() {
				return BigInteger.ZERO;
			}

			@Override
			public BigInteger getNullAsValueBridge2() {
				return BigInteger.valueOf( -10301 );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		BigInteger id;

	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		BigInteger id;

	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		BigInteger myProperty;
		@GenericField(indexNullAs = "0")
		BigInteger indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		BigInteger myProperty;
		@GenericField(indexNullAs = "-10301")
		BigInteger indexNullAsProperty;
	}
}
