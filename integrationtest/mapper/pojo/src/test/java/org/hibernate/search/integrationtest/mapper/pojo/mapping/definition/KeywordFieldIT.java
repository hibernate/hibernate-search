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
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeRef;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;

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
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void noNormalizer() {
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
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
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
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_implicitBinding() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue myProperty;
			IndexedEntity(int id, WrappedValue myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@KeywordField(normalizer = NORMALIZER_NAME, valueBridge = @ValueBridgeRef(type = ValidImplicitBindingBridge.class))
			public WrappedValue getMyProperty() {
				return myProperty;
			}
		}

		WrappedValue value = new WrappedValue();
		value.wrapped = "some value";
		doTestValidMapping(
				IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ),
				WrappedValue.class, String.class,
				value, value.wrapped
		);
	}

	@Test
	public void customBridge_explicitBinding() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			WrappedValue myProperty;
			IndexedEntity(int id, WrappedValue myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@KeywordField(normalizer = NORMALIZER_NAME, valueBridge = @ValueBridgeRef(type = ValidExplicitBindingBridge.class))
			public WrappedValue getMyProperty() {
				return myProperty;
			}
		}

		WrappedValue value = new WrappedValue();
		value.wrapped = "some value";
		doTestValidMapping(
				IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ),
				WrappedValue.class, String.class,
				value, value.wrapped
		);
	}

	@Test
	public void error_invalidFieldType_defaultBridge() {
		@Indexed
		class IndexedEntity {
			Integer id;
			Integer myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@KeywordField(normalizer = NORMALIZER_NAME)
			public Integer getMyProperty() {
				return myProperty;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"This property is mapped to a keyword field, but with a value bridge that creates a non-String or otherwise incompatible field",
								"bind() method returned context '",
								"expected '" + StringIndexFieldTypeContext.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	public void error_invalidFieldType_customBridge_explicitBinding() {
		@Indexed
		class IndexedEntity {
			Integer id;
			String myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@KeywordField(normalizer = NORMALIZER_NAME, valueBridge = @ValueBridgeRef(type = InvalidExplicitBindingBridge.class))
			public String getMyProperty() {
				return myProperty;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"This property is mapped to a keyword field, but with a value bridge that creates a non-String or otherwise incompatible field",
								"bind() method returned context '",
								"expected '" + StringIndexFieldTypeContext.class.getName() + "'"
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
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( entityType );
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

	public static class ValidImplicitBindingBridge implements ValueBridge<WrappedValue, String> {
		@Override
		public String toIndexedValue(WrappedValue value,
				ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}
		@Override
		public WrappedValue cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	public static class ValidExplicitBindingBridge implements ValueBridge<WrappedValue, String> {
		@Override
		public StandardIndexFieldTypeContext<?, String> bind(ValueBridgeBindingContext<WrappedValue> context) {
			return context.getTypeFactory().asString();
		}
		@Override
		public String toIndexedValue(WrappedValue value,
				ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}
		@Override
		public WrappedValue cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	private static class WrappedValue {
		private String wrapped;
	}

	public static class InvalidExplicitBindingBridge implements ValueBridge<String, Integer> {
		@Override
		public StandardIndexFieldTypeContext<?, Integer> bind(ValueBridgeBindingContext<String> context) {
			return context.getTypeFactory().asInteger();
		}
		@Override
		public Integer toIndexedValue(String value,
				ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
		@Override
		public String cast(Object value) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

}
