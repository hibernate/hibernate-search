/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link KeywordField} annotation.
 * <p>
 * Does not test error cases common to all kinds of {@code @XXField} annotations, which are tested in {@link FieldBaseIT}.
 * <p>
 * Does not test default bridges, which are tested in {@link FieldDefaultBridgeBaseIT}.
 * <p>
 * Does not test uses of container value extractors, which are tested in {@link FieldContainerExtractorBaseIT}
 * (and others, see javadoc on that class).
 */
public class KeywordFieldIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String NORMALIZER_NAME = "myNormalizer";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField
			String myProperty;
		}

		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", String.class )
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
			@KeywordField(name = "explicitName")
			String value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", String.class )
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
			@KeywordField(name = "invalid.withdot")
			String value;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( KeywordField.class )
						.failure( "Invalid index field name 'invalid.withdot': field names cannot contain a dot ('.')" ) );
	}

	@Test
	public void defaultBridge() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField(normalizer = NORMALIZER_NAME)
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
	public void norms() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@KeywordField(norms = Norms.YES)
			String norms;
			@KeywordField(norms = Norms.NO)
			String noNorms;
			@KeywordField(norms = Norms.DEFAULT)
			String defaultNorms;
			@KeywordField
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "norms", String.class, f -> f.norms( Norms.YES ) )
				.field( "noNorms", String.class, f -> f.norms( Norms.NO ) )
				.field( "defaultNorms", String.class )
				.field( "implicit", String.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void searchable() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@KeywordField(searchable = Searchable.YES)
			String searchable;
			@KeywordField(searchable = Searchable.NO)
			String unsearchable;
			@KeywordField(searchable = Searchable.DEFAULT)
			String useDefault;
			@KeywordField
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchable", String.class, f -> f.searchable( Searchable.YES ) )
				.field( "unsearchable", String.class, f -> f.searchable( Searchable.NO ) )
				.field( "useDefault", String.class )
				.field( "implicit", String.class )
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
			@KeywordField(aggregable = Aggregable.YES)
			String enabled;
			@KeywordField(aggregable = Aggregable.NO)
			String disabled;
			@KeywordField(aggregable = Aggregable.DEFAULT)
			String explicitDefault;
			@KeywordField
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
	public void customBridge_implicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField(normalizer = NORMALIZER_NAME,
					valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class, f -> f.normalizerName( NORMALIZER_NAME ) )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_explicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField(normalizer = NORMALIZER_NAME,
					valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class, f -> f.normalizerName( NORMALIZER_NAME ) )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_withParams_annotationMapping() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField(valueBinder = @ValueBinderRef(type = ParametricBridge.ParametricBinder.class,
					params = @Param(name = "fixedPrefix", value = "fixed-prefix-")))
			WrappedValue wrap;

			IndexedEntity() {
			}
			IndexedEntity(Integer id, WrappedValue wrap) {
				this.id = id;
				this.wrap = wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "wrap", String.class ) );
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
	public void customBridge_withParams_programmaticMapping() {
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

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "wrap", String.class ) );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> {
					builder.addEntityType( IndexedEntity.class );

					TypeMappingStep indexedEntity = builder.programmaticMapping().type( IndexedEntity.class );
					indexedEntity.indexed().index( INDEX_NAME );
					indexedEntity.property( "id" ).documentId();
					indexedEntity.property( "wrap" ).keywordField().valueBinder(
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
	public void defaultBridge_invalidFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField(normalizer = NORMALIZER_NAME)
			Integer notString;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".notString" )
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
	public void customBridge_implicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField(normalizer = NORMALIZER_NAME,
					valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
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
	public void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField(normalizer = NORMALIZER_NAME,
					valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
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
	public void customBridge_implicitFieldType_generic() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@KeywordField(valueBridge = @ValueBridgeRef(type = GenericTypeBridge.class))
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
				.field( "myProperty", indexedFieldType, b2 -> b2.normalizerName( NORMALIZER_NAME ) )
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

		@SuppressWarnings("uncheked")
		private static String extractFixedPrefix(ValueBindingContext<?> context) {
			return (String) context.param( "fixedPrefix" );
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
