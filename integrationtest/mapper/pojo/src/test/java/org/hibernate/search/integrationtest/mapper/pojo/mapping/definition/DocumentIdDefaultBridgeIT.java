/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanBackendMappingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.easymock.Capture;

/**
 * Test default identifier bridges for the {@code @DocumentId} annotation.
 */
@RunWith(Parameterized.class)
public class DocumentIdDefaultBridgeIT<I> {

	@Parameterized.Parameters(name = "{0}")
	public static Object[] types() {
		return PropertyTypeDescriptor.getAll().stream()
				.map( type -> new Object[] { type, type.getDefaultIdentifierBridgeExpectations() } )
				.toArray();
	}

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private DefaultIdentifierBridgeExpectations<I> expectations;
	private SearchMapping mapping;
	private StubIndexSchemaNode index1RootSchemaNode;
	private StubIndexSchemaNode index2RootSchemaNode;

	public DocumentIdDefaultBridgeIT(PropertyTypeDescriptor<I> typeDescriptor,
			Optional<DefaultIdentifierBridgeExpectations<I>> expectations) {
		Assume.assumeTrue(
				"Type " + typeDescriptor + " does not have a default identifier bridge", expectations.isPresent()
		);
		this.expectations = expectations.get();
	}

	@Before
	public void setup() {
		Capture<StubIndexSchemaNode> schemaCapture1 = Capture.newInstance();
		Capture<StubIndexSchemaNode> schemaCapture2 = Capture.newInstance();
		backendMock.expectSchema(
				DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_INDEX_NAME,
				b -> { },
				schemaCapture1
		);
		backendMock.expectSchema(
				DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_INDEX_NAME,
				b -> { },
				schemaCapture2
		);
		mapping = setupHelper.start()
				.setup( expectations.getTypeWithIdentifierBridge1(), expectations.getTypeWithIdentifierBridge2() );
		backendMock.verifyExpectationsMet();
		index1RootSchemaNode = schemaCapture1.getValue();
		index2RootSchemaNode = schemaCapture2.getValue();
	}

	@Test
	public void indexing() {
		try ( SearchSession session = mapping.createSession() ) {
			for ( I entityIdentifierValue : expectations.getEntityIdentifierValues() ) {
				Object entity = expectations.instantiateTypeWithIdentifierBridge1( entityIdentifierValue );
				session.getMainWorkPlan().add( entity );
			}

			BackendMock.DocumentWorkCallListContext expectationSetter = backendMock.expectWorks(
					DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_INDEX_NAME
			);
			for ( String expectedDocumentIdentifierValue : expectations.getDocumentIdentifierValues() ) {
				expectationSetter.add(
						expectedDocumentIdentifierValue,
						b -> { }
				);
			}
			expectationSetter.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void projection() {
		try ( SearchSession session = mapping.createSession() ) {
			Iterator<I> entityIdentifierIterator = expectations.getEntityIdentifierValues().iterator();
			for ( String documentIdentifierValue : expectations.getDocumentIdentifierValues() ) {
				I entityIdentifierValue = entityIdentifierIterator.next();
				backendMock.expectSearchReferences(
						Collections.singletonList(
								DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_INDEX_NAME ),
						b -> {
						},
						StubSearchWorkBehavior.of(
								1L,
								StubBackendUtils.reference(
										DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_INDEX_NAME,
										documentIdentifierValue
								)
						)
				);

				SearchQuery<EntityReference> query = session.search( expectations.getTypeWithIdentifierBridge1() )
						.asEntityReference()
						.predicate( f -> f.matchAll() )
						.toQuery();

				assertThat( query.fetch().getHits() )
						.containsExactly( new EntityReferenceImpl(
								expectations.getTypeWithIdentifierBridge1(),
								entityIdentifierValue
						) );
			}
			backendMock.verifyExpectationsMet();
		}
	}

	// Test behavior that backends expect from our bridges when using the DSLs
	@Test
	public void dslToIndexConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		ToDocumentIdentifierValueConverter<I> dslToIndexConverter =
				(ToDocumentIdentifierValueConverter<I>) index1RootSchemaNode.getIdDslConverter();
		ToDocumentIdentifierValueConverter<?> compatibleDslToIndexConverter =
				index2RootSchemaNode.getIdDslConverter();
		ToDocumentIdentifierValueConvertContextImpl convertContext =
				new ToDocumentIdentifierValueConvertContextImpl( new JavaBeanBackendMappingContext() );

		// isCompatibleWith must return true when appropriate
		assertThat( dslToIndexConverter.isCompatibleWith( dslToIndexConverter ) ).isTrue();
		assertThat( dslToIndexConverter.isCompatibleWith( compatibleDslToIndexConverter ) ).isTrue();
		assertThat( dslToIndexConverter.isCompatibleWith( new IncompatibleToDocumentIdentifierValueConverter() ) )
				.isFalse();

		// convert and convertUnknown must behave appropriately on valid input
		Iterator<String> documentIdentifierIterator = expectations.getDocumentIdentifierValues().iterator();
		for ( I entityIdentifierValue : expectations.getEntityIdentifierValues() ) {
			String documentIdentifierValue = documentIdentifierIterator.next();
			assertThat(
					dslToIndexConverter.convert( entityIdentifierValue, convertContext )
			)
					.isEqualTo( documentIdentifierValue );
			assertThat(
					dslToIndexConverter.convertUnknown( entityIdentifierValue, convertContext )
			)
					.isEqualTo( documentIdentifierValue );
		}

		// convertUnknown must throw a runtime exception on invalid input
		SubTest.expectException(
				"convertUnknown on invalid input",
				() -> dslToIndexConverter.convertUnknown( new Object(), convertContext )
		)
				.assertThrown()
				.isInstanceOf( RuntimeException.class );
	}

	private static class IncompatibleToDocumentIdentifierValueConverter
			implements ToDocumentIdentifierValueConverter<Object> {
		@Override
		public String convert(Object value, ToDocumentIdentifierValueConvertContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String convertUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCompatibleWith(ToDocumentIdentifierValueConverter<?> other) {
			throw new UnsupportedOperationException();
		}
	}
}
