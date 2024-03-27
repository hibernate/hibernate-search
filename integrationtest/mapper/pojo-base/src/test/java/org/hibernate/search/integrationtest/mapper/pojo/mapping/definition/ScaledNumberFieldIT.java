/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ScaledNumberFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
	void name() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
	void name_invalid_dot() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(name = "invalid.withdot")
			BigDecimal value;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( ScaledNumberField.class )
						.failure( "Invalid index field name 'invalid.withdot': field names cannot contain a dot ('.')" ) );
	}

	@Test
	void validDecimalScales_bigDecimals() {

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
	void validDecimalScales_bigIntegers() {

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
	void defaultBridge_invalidFieldType() {
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
				.satisfies( FailureReportUtils.hasFailureReport()
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
						) );
	}

	@Test
	void searchable() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
	void aggregable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
	void customBridge_implicitFieldType() {
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
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_explicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 3,
					valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", BigDecimal.class, f -> f.decimalScale( 3 ) )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_withParams_annotationMapping() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 2, valueBinder = @ValueBinderRef(type = ParametricBridge.ParametricBinder.class,
					params = {
							@Param(name = "unscaledVal", value = "773"),
							@Param(name = "scale", value = "2")
					}))
			WrappedValue wrap;

			IndexedEntity() {
			}

			IndexedEntity(Integer id, WrappedValue wrap) {
				this.id = id;
				this.wrap = wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "wrap", BigDecimal.class, f -> f.decimalScale( 2 ) ) );
		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity( 1, new WrappedValue( new BigInteger( "739" ), 2 ) );
			IndexedEntity entity2 = new IndexedEntity( 2, new WrappedValue( new BigInteger( "333" ), 2 ) );
			IndexedEntity entity3 = new IndexedEntity( 3, new WrappedValue( new BigInteger( "-773" ), 2 ) );

			SearchIndexingPlan plan = session.indexingPlan();
			plan.add( entity1 );
			plan.add( entity2 );
			plan.add( entity3 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "wrap", new BigDecimal( new BigInteger( "1512" ), 2 ) ) )
					.add( "2", b -> b.field( "wrap", new BigDecimal( new BigInteger( "1106" ), 2 ) ) )
					.add( "3", b -> b.field( "wrap", new BigDecimal( BigInteger.ZERO, 2 ) ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_withParams_programmaticMapping() {
		class IndexedEntity {
			Integer id;
			WrappedValue wrap;

			IndexedEntity() {
			}

			IndexedEntity(Integer id, WrappedValue wrap) {
				this.id = id;
				this.wrap = wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "wrap", BigDecimal.class, f -> f.decimalScale( 2 ) ) );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> {
					TypeMappingStep indexedEntity = builder.programmaticMapping().type( IndexedEntity.class );
					indexedEntity.searchEntity();
					indexedEntity.indexed().index( INDEX_NAME );
					indexedEntity.property( "id" ).documentId();
					indexedEntity.property( "wrap" ).scaledNumberField().decimalScale( 2 ).valueBinder(
							new ParametricBridge.ParametricBinder(),
							Collections.singletonMap( "baseDecimal", new BigDecimal( new BigInteger( "773" ), 2 ) )
					);
				} )
				.expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity( 1, new WrappedValue( new BigInteger( "739" ), 2 ) );
			IndexedEntity entity2 = new IndexedEntity( 2, new WrappedValue( new BigInteger( "333" ), 2 ) );
			IndexedEntity entity3 = new IndexedEntity( 3, new WrappedValue( new BigInteger( "-773" ), 2 ) );

			SearchIndexingPlan plan = session.indexingPlan();
			plan.add( entity1 );
			plan.add( entity2 );
			plan.add( entity3 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "wrap", new BigDecimal( new BigInteger( "1512" ), 2 ) ) )
					.add( "2", b -> b.field( "wrap", new BigDecimal( new BigInteger( "1106" ), 2 ) ) )
					.add( "3", b -> b.field( "wrap", new BigDecimal( BigInteger.ZERO, 2 ) ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_implicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 3, valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
			WrappedValue wrap;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
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
						) );
	}

	@Test
	void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(decimalScale = 3,
					valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
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
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	void customBridge_implicitFieldType_generic() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@ScaledNumberField(valueBridge = @ValueBridgeRef(type = GenericTypeBridge.class))
			String property;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".property" )
						.failure( "Unable to infer index field type for value bridge '"
								+ GenericTypeBridge.TOSTRING + "':"
								+ " this bridge implements ValueBridge<V, F>,"
								+ " but sets the generic type parameter F to 'T'."
								+ " The index field type can only be inferred automatically"
								+ " when this type parameter is set to a raw class."
								+ " Use a ValueBinder to set the index field type explicitly,"
								+ " or set the type parameter F to a definite, raw type." ) );
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

	public static class ParametricBridge implements ValueBridge<WrappedValue, BigDecimal> {

		private final BigDecimal baseDecimal;

		private ParametricBridge(BigDecimal baseDecimal) {
			this.baseDecimal = baseDecimal;
		}

		@Override
		public BigDecimal toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			return ( value == null ) ? baseDecimal : value.wrapped.add( baseDecimal );
		}

		public static class ParametricBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( WrappedValue.class, new ParametricBridge( extractBaseDecimal( context ) ),
						context.typeFactory().asBigDecimal()
				);
			}
		}

		private static BigDecimal extractBaseDecimal(ValueBindingContext<?> context) {
			Optional<BigDecimal> optionalBaseDecimal = context.paramOptional( "baseDecimal", BigDecimal.class );
			if ( optionalBaseDecimal.isPresent() ) {
				return optionalBaseDecimal.get();
			}

			String unscaledValParam = context.param( "unscaledVal", String.class );
			String scaleParam = context.param( "scale", String.class );
			BigInteger unscaledVal = new BigInteger( unscaledValParam );
			int scale = Integer.parseInt( scaleParam );
			return new BigDecimal( unscaledVal, scale );
		}
	}

	private static class WrappedValue {
		private BigDecimal wrapped;

		WrappedValue() {
		}

		WrappedValue(BigInteger unscaledValue, int scale) {
			this.wrapped = new BigDecimal( unscaledValue, scale );
		}
	}
}
