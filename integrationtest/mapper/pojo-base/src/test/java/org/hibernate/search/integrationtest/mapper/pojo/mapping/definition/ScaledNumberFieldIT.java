/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Rule;
import org.junit.Test;

public class ScaledNumberFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			Integer id;
			BigDecimal value;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@ScaledNumberField
			public BigDecimal getValue() {
				return value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "value", BigDecimal.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void name() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			Integer id;
			BigDecimal value;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@ScaledNumberField(name = "explicitName")
			public BigDecimal getValue() {
				return value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", BigDecimal.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void name_invalid_dot() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			Integer id;
			BigDecimal value;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@ScaledNumberField(name = "invalid.withdot")
			public BigDecimal getValue() {
				return value;
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( ScaledNumberField.class )
						.failure(
								"Index field name 'invalid.withdot' is invalid: field names cannot contain a dot ('.')"
						)
						.build()
				);
	}

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
		setupHelper.start().setup( IndexedEntity.class );
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
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void defaultBridge_invalidFieldType() {
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

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".notScalable" )
						.failure(
								"This property's mapping expects a scaled number type (BigDecimal or BigInteger) for the index field",
								"but the assigned value bridge or value binder declares a non-scaled type",
								"encountered type DSL step '",
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
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void aggregable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			Integer id;
			BigDecimal enabled;
			BigDecimal disabled;
			BigDecimal explicitDefault;
			BigDecimal implicitDefault;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(aggregable = Aggregable.YES)
			public BigDecimal getEnabled() {
				return enabled;
			}

			@ScaledNumberField(aggregable = Aggregable.NO)
			public BigDecimal getDisabled() {
				return disabled;
			}

			@ScaledNumberField(aggregable = Aggregable.DEFAULT)
			public BigDecimal getExplicitDefault() {
				return explicitDefault;
			}

			@ScaledNumberField
			public BigDecimal getImplicitDefault() {
				return implicitDefault;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "enabled", BigDecimal.class, f -> f.aggregable( Aggregable.YES ) )
				.field( "disabled", BigDecimal.class, f -> f.aggregable( Aggregable.NO ) )
				.field( "explicitDefault", BigDecimal.class )
				.field( "implicitDefault", BigDecimal.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_implicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 3, valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", BigDecimal.class, f -> f.decimalScale( 3 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_explicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 3, valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", BigDecimal.class, f -> f.decimalScale( 3 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_implicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 3, valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"This property's mapping expects a scaled number type (BigDecimal or BigInteger) for the index field",
								"but the assigned value bridge or value binder declares a non-scaled type",
								"encountered type DSL step '",
								"expected '" + ScaledNumberIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	public void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 3, valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"This property's mapping expects a scaled number type (BigDecimal or BigInteger) for the index field",
								"but the assigned value bridge or value binder declares a non-scaled type",
								"encountered type DSL step '",
								"expected '" + ScaledNumberIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	public static class ValidTypeBridge implements ValueBridge<WrappedValue, BigDecimal> {
		@Override
		public BigDecimal toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( WrappedValue.class, new ValidTypeBridge(), context.typeFactory().asBigDecimal() );
			}
		}
	}

	public static class InvalidTypeBridge implements ValueBridge<WrappedValue, Integer> {
		@Override
		public Integer toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( WrappedValue.class, new InvalidTypeBridge(), context.typeFactory().asInteger() );
			}
		}
	}

	private static class WrappedValue {
		private BigDecimal wrapped;
	}
}
