/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test default identifier bridges for the {@code @DocumentId} annotation.
 */
@RunWith(Parameterized.class)
public class DocumentIdDefaultBridgeBaseIT<I> {

	@Parameterized.Parameters(name = "{0}")
	public static Object[] types() {
		return PropertyTypeDescriptor.getAll().stream()
				.map( type -> new Object[] { type, type.getDefaultIdentifierBridgeExpectations() } )
				.toArray();
	}

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private final PropertyTypeDescriptor<I> typeDescriptor;
	private final DefaultIdentifierBridgeExpectations<I> expectations;
	private SearchMapping mapping;
	private StubIndexModel index1Model;
	private StubIndexModel index2Model;

	public DocumentIdDefaultBridgeBaseIT(PropertyTypeDescriptor<I> typeDescriptor,
			Optional<DefaultIdentifierBridgeExpectations<I>> expectations) {
		assumeTrue(
				"Type " + typeDescriptor + " does not have a default identifier bridge", expectations.isPresent()
		);
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations.get();
	}

	@Before
	public void setup() {
		backendMock.expectSchema(
				DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME,
				b -> { },
				indexModel -> this.index1Model = indexModel
		);
		backendMock.expectSchema(
				DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME,
				b -> { },
				indexModel -> this.index2Model = indexModel
		);
		mapping = setupHelper.start()
				.withAnnotatedEntityType( expectations.getTypeWithIdentifierBridge1(), DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME )
				.withAnnotatedEntityType( expectations.getTypeWithIdentifierBridge2(), DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME )
				.setup();
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indexing() {
		try ( SearchSession session = mapping.createSession() ) {
			for ( I entityIdentifierValue : expectations.getEntityIdentifierValues() ) {
				Object entity = expectations.instantiateTypeWithIdentifierBridge1( entityIdentifierValue );
				session.indexingPlan().add( entity );
			}

			BackendMock.DocumentWorkCallListContext expectationSetter = backendMock.expectWorks(
					DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME
			);
			for ( String expectedDocumentIdentifierValue : expectations.getDocumentIdentifierValues() ) {
				expectationSetter.add( expectedDocumentIdentifierValue, b -> { } );
			}
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void projection_entityReference() {
		try ( SearchSession session = mapping.createSession() ) {
			Iterator<I> entityIdentifierIterator = expectations.getEntityIdentifierValues().iterator();
			for ( String documentIdentifierValue : expectations.getDocumentIdentifierValues() ) {
				I entityIdentifierValue = entityIdentifierIterator.next();
				backendMock.expectSearchReferences(
						Collections.singletonList(
								DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME ),
						StubSearchWorkBehavior.of(
								1L,
								StubBackendUtils.reference(
										DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME,
										documentIdentifierValue
								)
						)
				);

				SearchQuery<EntityReference> query = session.search( expectations.getTypeWithIdentifierBridge1() )
						.selectEntityReference()
						.where( f -> f.matchAll() )
						.toQuery();

				assertThat( query.fetchAll().hits() )
						.containsExactly( EntityReferenceImpl.withName(
								expectations.getTypeWithIdentifierBridge1(),
								DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME,
								entityIdentifierValue
						) );
			}
			backendMock.verifyExpectationsMet();
		}
	}

	@Test
	public void projection_id() {
		try ( SearchSession session = mapping.createSession() ) {
			Iterator<I> entityIdentifierIterator = expectations.getEntityIdentifierValues().iterator();
			for ( String documentIdentifierValue : expectations.getDocumentIdentifierValues() ) {
				I entityIdentifierValue = entityIdentifierIterator.next();
				backendMock.expectSearchIds(
						Collections.singletonList(
								DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME ),
						b -> {
						},
						StubSearchWorkBehavior.of(
								1L,
								StubBackendUtils.reference(
										DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME,
										documentIdentifierValue
								)
						)
				);

				SearchQuery<Object> query = session.search( expectations.getTypeWithIdentifierBridge1() )
						.select( f -> f.id() )
						.where( f -> f.matchAll() )
						.toQuery();

				assertThat( query.fetchAll().hits() )
						.containsExactly( entityIdentifierValue );
			}
			backendMock.verifyExpectationsMet();
		}
	}

	// Test behavior that backends expect from our bridges when using the DSLs
	@Test
	public void dslConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		DslConverter<I, ?> dslConverter =
				(DslConverter<I, ?>) index1Model.identifier().dslConverter();
		DslConverter<?, ?> compatibleDslConverter =
				index2Model.identifier().dslConverter();
		DslConverter<?, ?> incompatibleDslConverter =
				new DslConverter<>( typeDescriptor.getJavaType(), new IncompatibleToDocumentValueConverter<>() );
		ToDocumentValueConvertContext convertContext =
				new ToDocumentValueConvertContextImpl( BridgeTestUtils.toBackendMappingContext( mapping ) );

		// isCompatibleWith must return true when appropriate
		assertThat( dslConverter.isCompatibleWith( dslConverter ) ).isTrue();
		assertThat( dslConverter.isCompatibleWith( compatibleDslConverter ) ).isTrue();
		assertThat( dslConverter.isCompatibleWith( incompatibleDslConverter ) ).isFalse();

		// conversion methods must behave appropriately on valid input
		Iterator<String> documentIdentifierIterator = expectations.getDocumentIdentifierValues().iterator();
		for ( I entityIdentifierValue : expectations.getEntityIdentifierValues() ) {
			String documentIdentifierValue = documentIdentifierIterator.next();
			assertThat(
					dslConverter.toDocumentValue( entityIdentifierValue, convertContext )
			)
					.isEqualTo( documentIdentifierValue );
			assertThat(
					dslConverter.unknownTypeToDocumentValue( entityIdentifierValue, convertContext )
			)
					.isEqualTo( documentIdentifierValue );
		}

		// conversion methods must throw a runtime exception on invalid input
		assertThatThrownBy(
				() -> dslConverter.unknownTypeToDocumentValue( new Object(), convertContext ),
				"convertUnknown on invalid input"
		)
				.isInstanceOf( RuntimeException.class );
	}

	// Test behavior that backends expect from our bridges when using the identifier projections
	@Test
	public void projectionConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		ProjectionConverter<String, I> projectionConverter =
				(ProjectionConverter<String, I>) index1Model.identifier().projectionConverter();
		ProjectionConverter<String, ?> compatibleProjectionConverter =
				index2Model.identifier().projectionConverter();
		ProjectionConverter<String, ?> incompatibleProjectionConverter =
				new ProjectionConverter<>( typeDescriptor.getJavaType(), new IncompatibleFromDocumentValueConverter<>() );

		// isCompatibleWith must return true when appropriate
		assertThat( projectionConverter.isCompatibleWith( projectionConverter ) ).isTrue();
		assertThat( projectionConverter.isCompatibleWith( compatibleProjectionConverter ) )
				.isTrue();
		assertThat( projectionConverter.isCompatibleWith( incompatibleProjectionConverter ) ).isFalse();

		// withConvertedType must return the same converter for compatible types and throw an exception for clearly incompatible types
		assertThatCode( () -> projectionConverter.withConvertedType( Object.class,
				() -> EventContexts.fromIndexFieldAbsolutePath( "foo" ) ) )
				.doesNotThrowAnyException();
		assertThatCode( () -> projectionConverter.withConvertedType( typeDescriptor.getBoxedJavaType(), () -> EventContexts.fromIndexFieldAbsolutePath( "foo" ) ) )
				.doesNotThrowAnyException();
		assertThatThrownBy( () -> projectionConverter.withConvertedType( IncompatibleType.class,
				() -> EventContexts.fromIndexFieldAbsolutePath( "foo" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type for returned values: '" + IncompatibleType.class.getName() + "'",
						"Expected '" + typeDescriptor.getBoxedJavaType().getName() + "' or a supertype",
						"Context: field 'foo'"
				);

		// conversion methods must behave appropriately on valid input
		try ( SearchSession searchSession = mapping.createSession() ) {
			FromDocumentValueConvertContext fromDocumentConvertContext =
					new FromDocumentValueConvertContextImpl(
							BridgeTestUtils.toBackendSessionContext( searchSession )
					);
			Iterator<I> projectionValuesIterator = expectations.getEntityIdentifierValues().iterator();
			for ( String documentIdentifierValue : expectations.getDocumentIdentifierValues() ) {
				I projectionValue = projectionValuesIterator.next();
				assertThat(
						projectionConverter.fromDocumentValue( documentIdentifierValue, fromDocumentConvertContext )
				)
						.isEqualTo( projectionValue );
			}
		}
	}

	/**
	 * A type that is clearly not a supertype of any type with a default bridge.
	 */
	private static final class IncompatibleType {
	}

	private static class IncompatibleToDocumentValueConverter<V>
			implements ToDocumentValueConverter<V, Object> {
		@Override
		public Object toDocumentValue(V value, ToDocumentValueConvertContext context) {
			throw new UnsupportedOperationException();
		}
	}

	private static class IncompatibleFromDocumentValueConverter<V>
			implements FromDocumentValueConverter<Object, V> {
		@Override
		public V fromDocumentValue(Object value, FromDocumentValueConvertContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
			throw new UnsupportedOperationException();
		}
	}
}
