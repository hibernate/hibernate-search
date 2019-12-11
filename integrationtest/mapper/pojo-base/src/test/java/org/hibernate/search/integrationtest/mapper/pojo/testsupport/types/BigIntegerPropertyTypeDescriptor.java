/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class BigIntegerPropertyTypeDescriptor extends PropertyTypeDescriptor<BigInteger> {

	BigIntegerPropertyTypeDescriptor() {
		super( BigInteger.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<BigInteger>> getDefaultIdentifierBridgeExpectations() {
		return Optional.of( new DefaultIdentifierBridgeExpectations<BigInteger>() {
			@Override
			public List<BigInteger> getEntityIdentifierValues() {
				return takeBigIntegerSequence();
			}

			@Override
			public List<String> getDocumentIdentifierValues() {
				return Arrays.asList(
						"-10000", "-1", "0", "1", "10", "10000", "9223372036854775807000" // Long.MAX_VALUE*1000
				);
			}

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
		} );
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<BigInteger, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<BigInteger, BigInteger>() {
			@Override
			public Class<BigInteger> getProjectionType() {
				return BigInteger.class;
			}

			@Override
			public Class<BigInteger> getIndexFieldJavaType() {
				return BigInteger.class;
			}

			@Override
			public List<BigInteger> getEntityPropertyValues() {
				return takeBigIntegerSequence();
			}

			@Override
			public List<BigInteger> getDocumentFieldValues() {
				return takeBigIntegerSequence();
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
		} );
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		BigInteger id;

		@DocumentId
		public BigInteger getId() {
			return id;
		}

	}
	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		BigInteger id;

		@DocumentId
		public BigInteger getId() {
			return id;
		}

	}
	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		BigInteger myProperty;
		BigInteger indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public BigInteger getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "0")
		public BigInteger getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		BigInteger myProperty;
		BigInteger indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public BigInteger getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "-10301")
		public BigInteger getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	private List<BigInteger> takeBigIntegerSequence() {
		return Arrays.asList(
				BigInteger.valueOf( -10000 ), BigInteger.valueOf( -1 ),
				BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN,
				BigInteger.valueOf( 10000 ),
				BigInteger.valueOf( Long.MAX_VALUE ).multiply( BigInteger.valueOf( 1000 ) )
		);
	}
}
