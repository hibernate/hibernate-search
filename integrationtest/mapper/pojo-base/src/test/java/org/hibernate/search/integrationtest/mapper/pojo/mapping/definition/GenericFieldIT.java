/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendExtension;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class GenericFieldIT {

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
			@GenericField
			Long value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "value", Long.class )
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
			@GenericField(name = "explicitName")
			LocalDate value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", LocalDate.class )
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
			@GenericField(name = "invalid.withdot")
			Long value;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( GenericField.class )
						.failure( "Invalid index field name 'invalid.withdot': field names cannot contain a dot ('.')" ) );
	}

	@Test
	void searchable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(searchable = Searchable.YES)
			Long searchable;
			@GenericField(searchable = Searchable.NO)
			LocalDate unsearchable;
			@GenericField(searchable = Searchable.DEFAULT)
			BigDecimal useDefault;
			@GenericField
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchable", Long.class, f -> f.searchable( Searchable.YES ) )
				.field( "unsearchable", LocalDate.class, f -> f.searchable( Searchable.NO ) )
				.field( "useDefault", BigDecimal.class )
				.field( "implicit", String.class )
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
			@GenericField(aggregable = Aggregable.YES)
			String enabled;
			@GenericField(aggregable = Aggregable.NO)
			String disabled;
			@GenericField(aggregable = Aggregable.DEFAULT)
			String explicitDefault;
			@GenericField
			String implicitDefault;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "enabled", String.class, f -> f.aggregable( Aggregable.YES ) )
				.field( "disabled", String.class, f -> f.aggregable( Aggregable.NO ) )
				.field( "explicitDefault", String.class )
				.field( "implicitDefault", String.class )
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
			@GenericField(valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class )
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
			@GenericField(valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class )
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
			@GenericField(valueBinder = @ValueBinderRef(type = ParametricBridge.ParametricBinder.class,
					params = @Param(name = "stringBase", value = "4")))
			Integer value;

			IndexedEntity() {
			}

			IndexedEntity(Integer id, Integer value) {
				this.id = id;
				this.value = value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "value", String.class ) );
		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan plan = session.indexingPlan();
			plan.add( new IndexedEntity( 1, 93 ) );
			plan.add( new IndexedEntity( 2, null ) );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "value", "1" ) )
					.add( "2", b -> b.field( "value", "4" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_withParams_paramNotDefined() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBinder = @ValueBinderRef(type = ParametricBridge.ParametricBinder.class))
			Integer value;

			IndexedEntity(Integer id, Integer value) {
				this.id = id;
				this.value = value;
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.failure( "Param with name 'stringBase' has not been defined for the binder." )
				);
	}

	@Test
	void customBridge_withParams_paramDefinedTwice() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBinder = @ValueBinderRef(type = ParametricBridge.ParametricBinder.class,
					params = { @Param(name = "stringBase", value = "4"), @Param(name = "stringBase", value = "4") }))
			Integer value;

			IndexedEntity(Integer id, Integer value) {
				this.id = id;
				this.value = value;
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( GenericField.class )
						.failure( "Conflicting usage of @Param annotation for parameter name: 'stringBase'. " +
								"Can't assign both value '4' and '4'" )
				);
	}

	@Test
	void customBridge_withParams_programmaticMapping() {
		class IndexedEntity {
			Integer id;
			Integer value;

			IndexedEntity() {
			}

			IndexedEntity(Integer id, Integer value) {
				this.id = id;
				this.value = value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "value", String.class ) );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> {
					TypeMappingStep indexedEntity = builder.programmaticMapping().type( IndexedEntity.class );
					indexedEntity.searchEntity();
					indexedEntity.indexed().index( INDEX_NAME );
					indexedEntity.property( "id" ).documentId();
					indexedEntity.property( "value" ).genericField().valueBinder(
							new ParametricBridge.ParametricBinder(),
							Collections.singletonMap( "base", 4 )
					);
				} )
				.expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan plan = session.indexingPlan();
			plan.add( new IndexedEntity( 1, 93 ) );
			plan.add( new IndexedEntity( 2, null ) );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "value", "1" ) )
					.add( "2", b -> b.field( "value", "4" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"Unable to apply property mapping: this property mapping must target an index field of standard type",
								"the resolved field type is non-standard",
								"This generally means you need to use a different field annotation"
										+ " or to convert property values using a custom ValueBridge or ValueBinder",
								"If you are already using a custom ValueBridge or ValueBinder, check its field type",
								"encountered type DSL step '",
								"expected interface '" + StandardIndexFieldTypeOptionsStep.class.getName() + "'"
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	void customBridge_implicitFieldType_generic() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = GenericTypeBridge.class))
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

	public static class ValidTypeBridge implements ValueBridge<WrappedValue, String> {
		@Override
		public String toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( WrappedValue.class, new ValidTypeBridge(), context.typeFactory().asString() );
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
				context.bridge(
						WrappedValue.class, new InvalidTypeBridge(),
						context.typeFactory().extension( StubBackendExtension.get() ).asNonStandard( Integer.class )
				);
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

	public static class ParametricBridge implements ValueBridge<Integer, String> {

		private final int base;

		private ParametricBridge(int base) {
			this.base = base;
		}

		@Override
		public String toIndexedValue(Integer value, ValueBridgeToIndexedValueContext context) {
			if ( value == null ) {
				return base + "";
			}

			return ( value % base ) + "";
		}

		public static class ParametricBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( Integer.class, new ParametricBridge( extractBase( context ) ),
						context.typeFactory().asString() );
			}
		}

		private static int extractBase(ValueBindingContext<?> context) {
			Optional<Integer> optionalBase = context.paramOptional( "base", Integer.class );
			if ( optionalBase.isPresent() ) {
				return optionalBase.get();
			}

			String stringBase = context.param( "stringBase", String.class );
			return Integer.parseInt( stringBase );
		}
	}

	private static class WrappedValue {
		private String wrapped;
	}

}
