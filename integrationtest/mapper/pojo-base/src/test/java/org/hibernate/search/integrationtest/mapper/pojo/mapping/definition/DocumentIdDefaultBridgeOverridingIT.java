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

import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
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
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private final PropertyTypeDescriptor<I, ?> typeDescriptor;
	private final DefaultIdentifierBridgeExpectations<I> expectations;
	private SearchMapping mapping;
	private StubIndexModel indexModel;

	public DocumentIdDefaultBridgeOverridingIT(PropertyTypeDescriptor<I, ?> typeDescriptor,
			DefaultIdentifierBridgeExpectations<I> expectations) {
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations;
	}

	@Before
	public void setup() {
		backendMock.expectSchema(
				DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME,
				b -> { },
				indexModel -> this.indexModel = indexModel
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
					.add( getDocumentIdentifierValue(), b -> { } );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void projection_entityReference() {
		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchReferences(
					Collections.singletonList( DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME ),
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

	@Test
	public void projection_id() {
		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchIds(
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

			SearchQuery<Object> query = session.search( expectations.getTypeWithIdentifierBridge1() )
					.select( f -> f.id() )
					.where( f -> f.matchAll() )
					.toQuery();

			assertThat( query.fetchAll().hits() )
					.containsExactly( getEntityIdentifierValue() );
			backendMock.verifyExpectationsMet();
		}
	}

	// Test behavior that backends expect from our bridges when using the DSLs
	@Test
	public void dslToIndexConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		DslConverter<I, ?> dslConverter =
				(DslConverter<I, ?>) indexModel.identifier().dslConverter();
		ToDocumentValueConvertContext convertContext =
				new ToDocumentValueConvertContextImpl( BridgeTestUtils.toBackendMappingContext( mapping ) );

		// The overriden default bridge must be used by the DSL converter
		assertThat( dslConverter.toDocumentValue( getEntityIdentifierValue(), convertContext ) )
				.isEqualTo( getDocumentIdentifierValue() );
		assertThat( dslConverter.unknownTypeToDocumentValue( getEntityIdentifierValue(), convertContext ) )
				.isEqualTo( getDocumentIdentifierValue() );
	}

	private I getEntityIdentifierValue() {
		return typeDescriptor.values().entityModelValues.get( 0 );
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
