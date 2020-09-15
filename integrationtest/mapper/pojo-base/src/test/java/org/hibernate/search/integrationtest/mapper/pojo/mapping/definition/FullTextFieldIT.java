/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Rule;
import org.junit.Test;

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
public class FullTextFieldIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String ANALYZER_NAME = "myAnalyzer";
	private static final String SEARCH_ANALYZER_NAME = "mySearchAnalyzer";

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
			@FullTextField(analyzer = ANALYZER_NAME)
			String value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "value", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
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
			@FullTextField(name = "explicitName", analyzer = ANALYZER_NAME)
			String value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
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
			@FullTextField(name = "invalid.withdot", analyzer = ANALYZER_NAME)
			String value;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( FullTextField.class )
						.failure(
								"Index field name 'invalid.withdot' is invalid: field names cannot contain a dot ('.')"
						)
						.build()
				);
	}

	@Test
	public void defaultBridge() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(analyzer = ANALYZER_NAME)
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
			@FullTextField(analyzer = ANALYZER_NAME, norms = Norms.YES)
			String norms;
			@FullTextField(analyzer = ANALYZER_NAME, norms = Norms.NO)
			String noNorms;
			@FullTextField(analyzer = ANALYZER_NAME, norms = Norms.DEFAULT)
			String defaultNorms;
			@FullTextField(analyzer = ANALYZER_NAME)
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "norms", String.class, f -> f.analyzerName( ANALYZER_NAME ).norms( Norms.YES ) )
				.field( "noNorms", String.class, f -> f.analyzerName( ANALYZER_NAME ).norms( Norms.NO ) )
				.field( "defaultNorms", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
				.field( "implicit", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
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
			@FullTextField(analyzer = ANALYZER_NAME, searchable = Searchable.YES)
			String searchable;
			@FullTextField(analyzer = ANALYZER_NAME, searchable = Searchable.NO)
			String unsearchable;
			@FullTextField(analyzer = ANALYZER_NAME, searchable = Searchable.DEFAULT)
			String useDefault;
			@FullTextField(analyzer = ANALYZER_NAME)
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchable", String.class, f -> f.analyzerName( ANALYZER_NAME ).searchable( Searchable.YES ) )
				.field( "unsearchable", String.class, f -> f.analyzerName( ANALYZER_NAME ).searchable( Searchable.NO ) )
				.field( "useDefault", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
				.field( "implicit", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void termVector() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@FullTextField(analyzer = ANALYZER_NAME, termVector = TermVector.YES)
			String termVector;
			@FullTextField(analyzer = ANALYZER_NAME, termVector = TermVector.NO)
			String noTermVector;
			@FullTextField(analyzer = ANALYZER_NAME, termVector = TermVector.WITH_POSITIONS_OFFSETS)
			String moreOptions;
			@FullTextField(analyzer = ANALYZER_NAME, searchable = Searchable.DEFAULT)
			String useDefault;
			@FullTextField(analyzer = ANALYZER_NAME)
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "termVector", String.class, f -> f.analyzerName( ANALYZER_NAME ).termVector( TermVector.YES ) )
				.field( "noTermVector", String.class, f -> f.analyzerName( ANALYZER_NAME ).termVector( TermVector.NO ) )
				.field( "moreOptions", String.class, f -> f.analyzerName( ANALYZER_NAME ).termVector( TermVector.WITH_POSITIONS_OFFSETS ) )
				.field( "useDefault", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
				.field( "implicit", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void searchAnalyzer() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@FullTextField(analyzer = ANALYZER_NAME, searchAnalyzer = SEARCH_ANALYZER_NAME)
			String text;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( ANALYZER_NAME ).searchAnalyzerName( SEARCH_ANALYZER_NAME ) )
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
			@FullTextField(analyzer = ANALYZER_NAME,
					valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
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
			@FullTextField(analyzer = ANALYZER_NAME,
					valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class, f -> f.analyzerName( ANALYZER_NAME ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void defaultBridge_invalidFieldType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(analyzer = ANALYZER_NAME)
			Integer myProperty;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"This property's mapping expects a standard String type for the index field",
								"but the assigned value bridge or value binder declares a non-standard or non-String type",
								"encountered type DSL step '",
								"expected '" + StringIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	public void customBridge_implicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@FullTextField(analyzer = ANALYZER_NAME,
					valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
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
								"This property's mapping expects a standard String type for the index field",
								"but the assigned value bridge or value binder declares a non-standard or non-String type",
								"encountered type DSL step '",
								"expected '" + StringIndexFieldTypeOptionsStep.class.getName() + "'"
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
			@FullTextField(analyzer = ANALYZER_NAME,
					valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
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
								"This property's mapping expects a standard String type for the index field",
								"but the assigned value bridge or value binder declares a non-standard or non-String type",
								"encountered type DSL step '",
								"expected '" + StringIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	private <E, P, F> void doTestValidMapping(Class<E> entityType,
			BiFunction<Integer, P, E> newEntityFunction,
			Class<P> propertyType, Class<F> indexedFieldType,
			P propertyValue, F indexedFieldValue) {
		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", indexedFieldType, b2 -> b2.analyzerName( ANALYZER_NAME ) )
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
					)
					.processedThenExecuted();
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

	private static class WrappedValue {
		private String wrapped;
	}

}
