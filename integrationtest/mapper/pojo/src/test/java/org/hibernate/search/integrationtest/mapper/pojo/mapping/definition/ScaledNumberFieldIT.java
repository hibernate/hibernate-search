/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public class ScaledNumberFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void validDecimalScales_bigDecimals() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			BigDecimal scaled;
			BigDecimal unscaled;
			BigDecimal defaultScaled;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 7)
			public BigDecimal getScaled() {
				return scaled;
			}

			@ScaledNumberField(decimalScale = 0)
			public BigDecimal getUnscaled() {
				return unscaled;
			}

			@ScaledNumberField
			public BigDecimal getDefaultScaled() {
				return defaultScaled;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "scaled", BigDecimal.class, f -> f.decimalScale( 7 ) )
				.field( "unscaled", BigDecimal.class, f -> f.decimalScale( 0 ) )
				.field( "defaultScaled", BigDecimal.class )
		);
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void validDecimalScales_bigIntegers() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			BigInteger scaled;
			BigInteger unscaled;
			BigInteger defaultScaled;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 7)
			public BigInteger getScaled() {
				return scaled;
			}

			@ScaledNumberField(decimalScale = 0)
			public BigInteger getUnscaled() {
				return unscaled;
			}

			@ScaledNumberField
			public BigInteger getDefaultScaled() {
				return defaultScaled;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "scaled", BigInteger.class, f -> f.decimalScale( 7 ) )
				.field( "unscaled", BigInteger.class, f -> f.decimalScale( 0 ) )
				.field( "defaultScaled", BigInteger.class )
		);
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void invalidFieldType() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Integer notScalable;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField
			public Integer getNotScalable() {
				return notScalable;
			}
		}

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".notScalable" )
						.failure(
								"This property is mapped to a scaled number field, but with a value bridge that creates neither a BigDecimal nor a BigInteger field.",
								"bind() method returned '",
								"expected '" + ScaledNumberIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	public void searchable() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			Integer id;
			BigDecimal searchable;
			BigInteger unsearchable;
			BigDecimal useDefault;
			BigInteger implicit;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(searchable = Searchable.YES)
			public BigDecimal getSearchable() {
				return searchable;
			}

			@ScaledNumberField(searchable = Searchable.NO)
			public BigInteger getUnsearchable() {
				return unsearchable;
			}

			@ScaledNumberField(searchable = Searchable.DEFAULT)
			public BigDecimal getUseDefault() {
				return useDefault;
			}

			@ScaledNumberField
			public BigInteger getImplicit() {
				return implicit;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchable", BigDecimal.class, f -> f.searchable( Searchable.YES ) )
				.field( "unsearchable", BigInteger.class, f -> f.searchable( Searchable.NO ) )
				.field( "useDefault", BigDecimal.class )
				.field( "implicit", BigInteger.class )
		);
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_implicitBinding() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 3, valueBridge = @ValueBridgeRef(type = ValidImplicitBindingBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", BigDecimal.class, f -> f.decimalScale( 3 ) )
		);
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_explicitBinding() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 3, valueBridge = @ValueBridgeRef(type = ValidExplicitBindingBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", BigDecimal.class, f -> f.decimalScale( 3 ) )
		);
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_invalidBinding() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			BigDecimal scaled;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 3, valueBridge = @ValueBridgeRef(type = InvalidExplicitBindingBridge.class))
			public BigDecimal getScaled() {
				return scaled;
			}
		}

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".scaled" )
						.failure(
								"This property is mapped to a scaled number field, but with a value bridge that creates neither a BigDecimal nor a BigInteger field.",
								"bind() method returned '",
								"expected '" + ScaledNumberIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	public static class ValidImplicitBindingBridge implements ValueBridge<WrappedValue, BigDecimal> {

		@Override
		public BigDecimal toIndexedValue(WrappedValue value,
				ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}
		@Override
		public WrappedValue cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	public static class ValidExplicitBindingBridge implements ValueBridge<WrappedValue, BigDecimal> {

		@Override
		public StandardIndexFieldTypeOptionsStep<?, BigDecimal> bind(ValueBridgeBindingContext<WrappedValue> context) {
			return context.getTypeFactory().asBigDecimal();
		}
		@Override
		public BigDecimal toIndexedValue(WrappedValue value,
				ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}
		@Override
		public WrappedValue cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	public static class InvalidExplicitBindingBridge implements ValueBridge<BigDecimal, Integer> {

		@Override
		public StandardIndexFieldTypeOptionsStep<?, Integer> bind(ValueBridgeBindingContext<BigDecimal> context) {
			return context.getTypeFactory().asInteger();
		}
		@Override
		public Integer toIndexedValue(BigDecimal value,
				ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
		@Override
		public BigDecimal cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	private static class WrappedValue {
		private BigDecimal wrapped;
	}
}
