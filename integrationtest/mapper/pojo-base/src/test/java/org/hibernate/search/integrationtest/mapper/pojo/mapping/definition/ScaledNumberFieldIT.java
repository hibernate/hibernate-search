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
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class ScaledNumberFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@ScaledNumberField
			BigDecimal value;
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
			@DocumentId
			Integer id;
			@ScaledNumberField(name = "explicitName")
			BigDecimal value;
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
			@DocumentId
			Integer id;
			@ScaledNumberField(name = "invalid.withdot")
			BigDecimal value;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( ScaledNumberField.class )
						.failure( "Invalid index field name 'invalid.withdot': field names cannot contain a dot ('.')" )
						.build()
				);
	}

	@Test
	public void validDecimalScales_bigDecimals() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 7)
			BigDecimal scaled;
			@ScaledNumberField(decimalScale = 0)
			BigDecimal unscaled;
			@ScaledNumberField
			BigDecimal defaultScaled;
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
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 7)
			BigInteger scaled;
			@ScaledNumberField(decimalScale = 0)
			BigInteger unscaled;
			@ScaledNumberField
			BigInteger defaultScaled;
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
			@DocumentId
			Integer id;
			@ScaledNumberField
			Integer notScalable;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".notScalable" )
						.failure(
								"Unable to apply property mapping: this property mapping must target"
										+ " an index field of standard, scaled-number type (BigDecimal or BigInteger)",
								"but the resolved field type is non-standard or non-scaled",
								"This generally means you need to use a different field annotation"
										+ " or to convert property values using a custom ValueBridge or ValueBinder.",
								"If you are already using a custom ValueBridge or ValueBinder, check its field type.",
								"encountered type DSL step '",
								"expected interface '" + ScaledNumberIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	public void searchable() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@ScaledNumberField(searchable = Searchable.YES)
			BigDecimal searchable;
			@ScaledNumberField(searchable = Searchable.NO)
			BigInteger unsearchable;
			@ScaledNumberField(searchable = Searchable.DEFAULT)
			BigDecimal useDefault;
			@ScaledNumberField
			BigInteger implicit;
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
			@DocumentId
			Integer id;
			@ScaledNumberField(aggregable = Aggregable.YES)
			BigDecimal enabled;
			@ScaledNumberField(aggregable = Aggregable.NO)
			BigDecimal disabled;
			@ScaledNumberField(aggregable = Aggregable.DEFAULT)
			BigDecimal explicitDefault;
			@ScaledNumberField
			BigDecimal implicitDefault;
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
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 3, valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			WrappedValue wrap;
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
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 3, valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
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
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 3, valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
			WrappedValue wrap;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"Unable to apply property mapping: this property mapping must target"
										+ " an index field of standard, scaled-number type (BigDecimal or BigInteger)",
								"but the resolved field type is non-standard or non-scaled",
								"This generally means you need to use a different field annotation"
										+ " or to convert property values using a custom ValueBridge or ValueBinder.",
								"If you are already using a custom ValueBridge or ValueBinder, check its field type.",
								"encountered type DSL step '",
								"expected interface '" + ScaledNumberIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	public void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 3, valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"Unable to apply property mapping: this property mapping must target"
										+ " an index field of standard, scaled-number type (BigDecimal or BigInteger)",
								"but the resolved field type is non-standard or non-scaled",
								"This generally means you need to use a different field annotation"
										+ " or to convert property values using a custom ValueBridge or ValueBinder.",
								"If you are already using a custom ValueBridge or ValueBinder, check its field type.",
								"encountered type DSL step '",
								"expected interface '" + ScaledNumberIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	public void customBridge_implicitFieldType_generic() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(valueBridge = @ValueBridgeRef(type = GenericTypeBridge.class))
			String property;
		}

		assertThatThrownBy( () -> setupHelper.start().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".property" )
						.failure( "Unable to infer index field type for value bridge '"
								+ GenericTypeBridge.TOSTRING + "':"
								+ " this bridge implements ValueBridge<V, F>,"
								+ " but sets the generic type parameter F to 'T'."
								+ " The index field type can only be inferred automatically"
								+ " when this type parameter is set to a raw class."
								+ " Use a ValueBinder to set the index field type explicitly,"
								+ " or set the type parameter F to a definite, raw type." )
						.build() );
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

	public static class GenericTypeBridge<T> implements ValueBridge<String, T> {
		private static final String TOSTRING = "<GenericTypeBridge toString() result>";

		@Override
		public String toString() {
			return TOSTRING;
		}

		@Override
		public T toIndexedValue(String value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	private static class WrappedValue {
		private BigDecimal wrapped;
	}
}
