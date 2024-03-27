/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test common use cases of the {@link FullTextField} annotation.
 * <p>
 * Does not test error cases common to all kinds of {@code @XXField} annotations, which are tested in {@link FieldBaseIT}.
 * <p>
 * Does not test default bridges, which are tested in {@link FieldDefaultBridgeBaseIT}.
 * <p>
 * Does not test uses of container value extractors, which are tested in {@link FieldContainerExtractorBaseIT}
 * (and others, see javadoc on that class).
 */
class FullTextFieldIT {

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
			@FullTextField
			String value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "value", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4123")
	void defaultAttributes_programmaticAPI() {
		class IndexedEntity {
			Integer id;
			String value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "value", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		setupHelper.start().withConfiguration( b -> {
			ProgrammaticMappingConfigurationContext mapping = b.programmaticMapping();

			TypeMappingStep type = mapping.type( IndexedEntity.class );
			type.indexed().index( INDEX_NAME );
			type.property( "id" ).documentId();
			type.property( "value" ).fullTextField();
		} ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void name() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(name = "explicitName")
			String value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
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
			@FullTextField(name = "invalid.withdot")
			String value;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( FullTextField.class )
						.failure( "Invalid index field name 'invalid.withdot': field names cannot contain a dot ('.')" ) );
	}

	@Test
	void defaultBridge() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField
			String myProperty;

			IndexedEntity(int id, String myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}
		}

		String value = "some value";
		doTestValidMapping(
				IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ),
				String.class, String.class,
				value, value
		);
	}

	@Test
	void norms() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(norms = Norms.YES)
			String norms;
			@FullTextField(norms = Norms.NO)
			String noNorms;
			@FullTextField(norms = Norms.DEFAULT)
			String defaultNorms;
			@FullTextField
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "norms", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ).norms( Norms.YES ) )
				.field( "noNorms", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ).norms( Norms.NO ) )
				.field( "defaultNorms", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.field( "implicit", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void searchable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(searchable = Searchable.YES)
			String searchable;
			@FullTextField(searchable = Searchable.NO)
			String unsearchable;
			@FullTextField(searchable = Searchable.DEFAULT)
			String useDefault;
			@FullTextField
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchable", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ).searchable( Searchable.YES ) )
				.field( "unsearchable", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ).searchable( Searchable.NO ) )
				.field( "useDefault", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.field( "implicit", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void termVector() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(termVector = TermVector.YES)
			String termVector;
			@FullTextField(termVector = TermVector.NO)
			String noTermVector;
			@FullTextField(termVector = TermVector.WITH_POSITIONS_OFFSETS)
			String moreOptions;
			@FullTextField(searchable = Searchable.DEFAULT)
			String useDefault;
			@FullTextField
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "termVector", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ).termVector( TermVector.YES ) )
				.field( "noTermVector", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ).termVector( TermVector.NO ) )
				.field( "moreOptions", String.class,
						f -> f.analyzerName( AnalyzerNames.DEFAULT ).termVector( TermVector.WITH_POSITIONS_OFFSETS ) )
				.field( "useDefault", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.field( "implicit", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void analyzer() {
		final String analyzerName = "analyzerName";
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(analyzer = analyzerName)
			String text;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( analyzerName ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void searchAnalyzer() {
		final String searchAnalyzerName = "searchAnalyzerName";
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(searchAnalyzer = searchAnalyzerName)
			String text;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT )
						.searchAnalyzerName( searchAnalyzerName ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void analyzer_searchAnalyzer() {
		final String analyzerName = "analyzerName";
		final String searchAnalyzerName = "searchAnalyzerName";
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(analyzer = analyzerName, searchAnalyzer = searchAnalyzerName)
			String text;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( analyzerName )
						.searchAnalyzerName( searchAnalyzerName ) )
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
			@FullTextField(valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
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
			@FullTextField(valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
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
			@FullTextField(valueBinder = @ValueBinderRef(type = ParametricBridge.ParametricBinder.class,
					params = @Param(name = "fixedPrefix", value = "fixed-prefix-")))
			WrappedValue wrap;

			IndexedEntity() {
			}

			IndexedEntity(Integer id, WrappedValue wrap) {
				this.id = id;
				this.wrap = wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "wrap", String.class, f -> f.analyzerName( "default" ) ) );
		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity( 1, new WrappedValue( "bla-bla-bla" ) );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "wrap", "fixed-prefix-bla-bla-bla" ) );
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

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "wrap", String.class, f -> f.analyzerName( "default" ) ) );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> {
					TypeMappingStep indexedEntity = builder.programmaticMapping().type( IndexedEntity.class );
					indexedEntity.searchEntity();
					indexedEntity.indexed().index( INDEX_NAME );
					indexedEntity.property( "id" ).documentId();
					indexedEntity.property( "wrap" ).fullTextField().valueBinder(
							new ParametricBridge.ParametricBinder(),
							Collections.singletonMap( "fixedPrefix", "fixed-prefix-" )
					);
				} )
				.expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity( 1, new WrappedValue( "bla-bla-bla" ) );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "wrap", "fixed-prefix-bla-bla-bla" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void defaultBridge_invalidFieldType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField
			Integer myProperty;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"Unable to apply property mapping: this property mapping must target an index field of standard String type",
								"the resolved field type is non-standard or non-String",
								"This generally means you need to use a different field annotation"
										+ " or to convert property values using a custom ValueBridge or ValueBinder",
								"If you are already using a custom ValueBridge or ValueBinder, check its field type",
								"encountered type DSL step '",
								"expected interface '" + StringIndexFieldTypeOptionsStep.class.getName() + "'"
						) );
	}

	@Test
	void customBridge_implicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
			WrappedValue wrap;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"Unable to apply property mapping: this property mapping must target an index field of standard String type",
								"the resolved field type is non-standard or non-String",
								"This generally means you need to use a different field annotation"
										+ " or to convert property values using a custom ValueBridge or ValueBinder",
								"If you are already using a custom ValueBridge or ValueBinder, check its field type",
								"encountered type DSL step '",
								"expected interface '" + StringIndexFieldTypeOptionsStep.class.getName() + "'"
						) );
	}

	@Test
	void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"Unable to apply property mapping: this property mapping must target an index field of standard String type",
								"the resolved field type is non-standard or non-String",
								"This generally means you need to use a different field annotation"
										+ " or to convert property values using a custom ValueBridge or ValueBinder",
								"If you are already using a custom ValueBridge or ValueBinder, check its field type",
								"encountered type DSL step '",
								"expected interface '" + StringIndexFieldTypeOptionsStep.class.getName() + "'"
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	void customBridge_implicitFieldType_generic() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(valueBridge = @ValueBridgeRef(type = GenericTypeBridge.class))
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

	private <E, P, F> void doTestValidMapping(Class<E> entityType,
			BiFunction<Integer, P, E> newEntityFunction,
			Class<P> propertyType, Class<F> indexedFieldType,
			P propertyValue, F indexedFieldValue) {
		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", indexedFieldType, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		SearchMapping mapping = setupHelper.start().setup( entityType );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = newEntityFunction.apply( 1, propertyValue );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.field( "myProperty", indexedFieldValue )
					);
		}
		backendMock.verifyExpectationsMet();
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

	public static class ParametricBridge implements ValueBridge<WrappedValue, String> {

		private final String fixedPrefix;

		private ParametricBridge(String fixedPrefix) {
			this.fixedPrefix = ( fixedPrefix == null ) ? "" : fixedPrefix;
		}

		@Override
		public String toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			return ( value == null ) ? fixedPrefix : fixedPrefix + value.wrapped;
		}

		public static class ParametricBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( WrappedValue.class, new ParametricBridge( extractFixedPrefix( context ) ),
						context.typeFactory().asString() );
			}
		}

		private static String extractFixedPrefix(ValueBindingContext<?> context) {
			return context.param( "fixedPrefix", String.class );
		}
	}

	private static class WrappedValue {
		private String wrapped;

		WrappedValue() {
		}

		WrappedValue(String wrapped) {
			this.wrapped = wrapped;
		}
	}
}
