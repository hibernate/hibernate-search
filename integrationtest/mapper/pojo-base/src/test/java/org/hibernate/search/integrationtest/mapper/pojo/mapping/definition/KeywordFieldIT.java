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

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Searchable;
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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.assertj.core.api.Assertions;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link KeywordField} annotation.
 * <p>
 * Does not test error cases common to all kinds of {@code @XXField} annotations, which are tested in {@link FieldBaseIT}.
 * <p>
 * Does not test default bridges, which are tested in {@link FieldDefaultBridgeIT}.
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
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			String myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@KeywordField
			public String getMyProperty() {
				return myProperty;
			}
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
			Integer id;
			String value;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@KeywordField(name = "explicitName")
			public String getValue() {
				return value;
			}
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
			Integer id;
			String value;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@KeywordField(name = "invalid.withdot")
			public String getValue() {
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
						.annotationContextAnyParameters( KeywordField.class )
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
			@KeywordField(normalizer = NORMALIZER_NAME)
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

			@KeywordField(norms = Norms.YES)
			public String getNorms() {
				return norms;
			}

			@KeywordField(norms = Norms.NO)
			public String getNoNorms() {
				return noNorms;
			}

			@KeywordField(norms = Norms.DEFAULT)
			public String getDefaultNorms() {
				return defaultNorms;
			}

			@KeywordField
			public String getImplicit() {
				return implicit;
			}
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
			Integer id;
			String searchable;
			String unsearchable;
			String useDefault;
			String implicit;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@KeywordField(searchable = Searchable.YES)
			public String getSearchable() {
				return searchable;
			}

			@KeywordField(searchable = Searchable.NO)
			public String getUnsearchable() {
				return unsearchable;
			}

			@KeywordField(searchable = Searchable.DEFAULT)
			public String getUseDefault() {
				return useDefault;
			}

			@KeywordField
			public String getImplicit() {
				return implicit;
			}
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
			Integer id;
			String enabled;
			String disabled;
			String explicitDefault;
			String implicitDefault;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@KeywordField(aggregable = Aggregable.YES)
			public String getEnabled() {
				return enabled;
			}

			@KeywordField(aggregable = Aggregable.NO)
			public String getDisabled() {
				return disabled;
			}

			@KeywordField(aggregable = Aggregable.DEFAULT)
			public String getExplicitDefault() {
				return explicitDefault;
			}

			@KeywordField
			public String getImplicitDefault() {
				return implicitDefault;
			}
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@KeywordField(normalizer = NORMALIZER_NAME,
					valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class, f -> f.normalizerName( NORMALIZER_NAME ) )
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

			@KeywordField(normalizer = NORMALIZER_NAME,
					valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class, f -> f.normalizerName( NORMALIZER_NAME ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void defaultBridge_invalidFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Integer notString;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@KeywordField(normalizer = NORMALIZER_NAME)
			public Integer getNotString() {
				return notString;
			}
		}

		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".notString" )
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@KeywordField(normalizer = NORMALIZER_NAME,
					valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		Assertions.assertThatThrownBy(
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@KeywordField(normalizer = NORMALIZER_NAME,
					valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		Assertions.assertThatThrownBy(
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
