/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test overriding default identifier bridges for the {@code @DocumentId} annotation,
 * for example assigning a different default identifier bridge for properties of type {@link String}.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3096")
public class DocumentIdDefaultBridgeOverridingIT<I> {

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
	private StubIndexSchemaNode rootSchemaNode;

	public DocumentIdDefaultBridgeOverridingIT(PropertyTypeDescriptor<I> typeDescriptor,
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
				schema -> this.rootSchemaNode = schema
		);
		mapping = setupHelper.start()
				.withAnnotatedEntityType( expectations.getTypeWithIdentifierBridge1(),
						DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME )
				// HERE we override the default bridge for the type being tested.
				.withConfiguration( builder -> builder.bridges().exactType( typeDescriptor.getJavaType() )
						.identifierBridge( new OverridingDefaultBridge() ) )
				.setup();
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indexing() {
		try ( SearchSession session = mapping.createSession() ) {
			Object entity = expectations.instantiateTypeWithIdentifierBridge1( getEntityIdentifierValue() );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME )
					.add( getDocumentIdentifierValue(), b -> { } )
					.createdThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void projection() {
		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchReferences(
					Collections.singletonList( DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							StubBackendUtils.reference(
									DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME,
									getDocumentIdentifierValue()
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
							getEntityIdentifierValue()
					) );
			backendMock.verifyExpectationsMet();
		}
	}

	// Test behavior that backends expect from our bridges when using the DSLs
	@Test
	public void dslToIndexConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		DocumentIdentifierValueConverter<I> dslToIndexConverter =
				(DocumentIdentifierValueConverter<I>) rootSchemaNode.getIdDslConverter();
		ToDocumentIdentifierValueConvertContextImpl convertContext =
				new ToDocumentIdentifierValueConvertContextImpl( BridgeTestUtils.toBackendMappingContext( mapping ) );

		// The overriden default bridge must be used by the DSL converter
		assertThat( dslToIndexConverter.convertToDocument( getEntityIdentifierValue(), convertContext ) )
				.isEqualTo( getDocumentIdentifierValue() );
		assertThat( dslToIndexConverter.convertToDocumentUnknown( getEntityIdentifierValue(), convertContext ) )
				.isEqualTo( getDocumentIdentifierValue() );
	}

	private I getEntityIdentifierValue() {
		return expectations.getEntityIdentifierValues().get( 0 );
	}

	private String getDocumentIdentifierValue() {
		// See OverridingDefaultBridge
		return "OVERRIDDEN_ID";
	}

	private class OverridingDefaultBridge implements IdentifierBridge<I> {
		private static final String DOCUMENT_ID = "OVERRIDDEN_ID";

		@Override
		public I fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			// We only support one ID value: this is just a stub
			return getEntityIdentifierValue();
		}

		@Override
		public String toDocumentIdentifier(I propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			// We only support one ID value: this is just a stub
			return DOCUMENT_ID;
		}
	}
}
