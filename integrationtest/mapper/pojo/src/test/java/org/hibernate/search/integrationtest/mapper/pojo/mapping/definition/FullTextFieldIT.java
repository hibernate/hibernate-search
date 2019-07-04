/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
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
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link FullTextField} annotation.
 * <p>
 * Does not test error cases common to all kinds of {@code @XXField} annotations, which are tested in {@link FieldBaseIT}.
 * <p>
 * Does not test default bridges, which are tested in {@link FieldDefaultBridgeIT}.
 * <p>
 * Does not test uses of container value extractors, which are tested in {@link FieldContainerExtractorBaseIT}
 * (and others, see javadoc on that class).
 */
public class FullTextFieldIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String ANALYZER_NAME = "myAnalyzer";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void defaultBridge() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			String myProperty;
			IndexedEntity(int id, String myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@FullTextField(analyzer = ANALYZER_NAME)
			public String getMyProperty() {
				return myProperty;
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
			Integer id;
			String norms;
			String noNorms;
			String defaultNorms;
			String implicit;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@FullTextField(analyzer = ANALYZER_NAME, norms = Norms.YES)
			public String getNorms() {
				return norms;
			}

			@FullTextField(analyzer = ANALYZER_NAME, norms = Norms.NO)
			public String getNoNorms() {
				return noNorms;
			}

			@FullTextField(analyzer = ANALYZER_NAME, norms = Norms.DEFAULT)
			public String getDefaultNorms() {
				return defaultNorms;
			}

			@FullTextField(analyzer = ANALYZER_NAME)
			public String getImplicit() {
				return implicit;
			}
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
			Integer id;
			String searchable;
			String unsearchable;
			String useDefault;
			String implicit;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@FullTextField(analyzer = ANALYZER_NAME, searchable = Searchable.YES)
			public String getSearchable() {
				return searchable;
			}

			@FullTextField(analyzer = ANALYZER_NAME, searchable = Searchable.NO)
			public String getUnsearchable() {
				return unsearchable;
			}

			@FullTextField(analyzer = ANALYZER_NAME, searchable = Searchable.DEFAULT)
			public String getUseDefault() {
				return useDefault;
			}

			@FullTextField(analyzer = ANALYZER_NAME)
			public String getImplicit() {
				return implicit;
			}
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
			Integer id;
			String termVector;
			String noTermVector;
			String moreOptions;
			String useDefault;
			String implicit;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@FullTextField(analyzer = ANALYZER_NAME, termVector = TermVector.YES)
			public String getTermVector() {
				return termVector;
			}

			@FullTextField(analyzer = ANALYZER_NAME, termVector = TermVector.NO)
			public String getNoTermVector() {
				return noTermVector;
			}

			@FullTextField(analyzer = ANALYZER_NAME, termVector = TermVector.WITH_POSITIONS_OFFSETS)
			public String getMoreOptions() {
				return moreOptions;
			}

			@FullTextField(analyzer = ANALYZER_NAME, searchable = Searchable.DEFAULT)
			public String getUseDefault() {
				return useDefault;
			}

			@FullTextField(analyzer = ANALYZER_NAME)
			public String getImplicit() {
				return implicit;
			}
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
	public void customBridge_implicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@FullTextField(analyzer = ANALYZER_NAME,
					valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@FullTextField(analyzer = ANALYZER_NAME,
					valueBridge = @ValueBridgeRef(binderType = ValidTypeBridge.ExplictFieldTypeBinder.class))
			public WrappedValue getWrap() {
				return wrap;
			}
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
			Integer id;
			Integer myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@FullTextField(analyzer = ANALYZER_NAME)
			public Integer getMyProperty() {
				return myProperty;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"This property is mapped to a full-text field, but with a value bridge that binds to a non-String or otherwise incompatible field",
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@FullTextField(analyzer = ANALYZER_NAME,
					valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"This property is mapped to a full-text field, but with a value bridge that binds to a non-String or otherwise incompatible field",
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@FullTextField(analyzer = ANALYZER_NAME,
					valueBridge = @ValueBridgeRef(binderType = InvalidTypeBridge.ExplictFieldTypeBinder.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"This property is mapped to a full-text field, but with a value bridge that binds to a non-String or otherwise incompatible field",
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
		JavaBeanMapping mapping = setupHelper.start().setup( entityType );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = newEntityFunction.apply( 1, propertyValue );

			session.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.field( "myProperty", indexedFieldValue )
					)
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	public static class ValidTypeBridge implements ValueBridge<WrappedValue, String> {
		@Override
		public String toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}

		@Override
		public WrappedValue cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		public static class ExplictFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.setBridge( WrappedValue.class, new ValidTypeBridge(), context.getTypeFactory().asString() );
			}
		}
	}

	public static class InvalidTypeBridge implements ValueBridge<WrappedValue, Integer> {
		@Override
		public Integer toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		@Override
		public WrappedValue cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		public static class ExplictFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.setBridge( WrappedValue.class, new InvalidTypeBridge(), context.getTypeFactory().asInteger() );
			}
		}
	}

	private static class WrappedValue {
		private String wrapped;
	}

}
