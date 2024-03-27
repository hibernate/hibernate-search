/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test overriding default identifier bridges for the {@code @DocumentId} annotation,
 * for example assigning a different default identifier bridge for properties of type {@link String}.
 */
@TestForIssue(jiraKey = "HSEARCH-3096")
class DocumentIdDefaultBridgeOverridingIT<I> {

	public static List<? extends Arguments> params() {
		return PropertyTypeDescriptor.getAll().stream()
				.map( type -> Arguments.of( type, type.getDefaultIdentifierBridgeExpectations() ) )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;
	private StubIndexModel indexModel;

	public void setup(PropertyTypeDescriptor<I, ?> typeDescriptor, DefaultIdentifierBridgeExpectations<I> expectations) {
		backendMock.expectSchema(
				DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME,
				b -> {},
				indexModel -> this.indexModel = indexModel
		);
		mapping = setupHelper.start()
				.withAnnotatedTypes( expectations.getTypeWithIdentifierBridge1() )
				.withConfiguration( b -> {
					b.programmaticMapping().type( expectations.getTypeWithIdentifierBridge1() )
							.searchEntity().name( DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME );
				} )
				// HERE we override the default bridge for the type being tested.
				.withConfiguration( builder -> builder.bridges().exactType( typeDescriptor.getJavaType() )
						.identifierBridge( new OverridingDefaultBridge( typeDescriptor ) ) )
				.setup();
		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void indexing(PropertyTypeDescriptor<I, ?> typeDescriptor, DefaultIdentifierBridgeExpectations<I> expectations) {
		setup( typeDescriptor, expectations );
		try ( SearchSession session = mapping.createSession() ) {
			Object entity = expectations.instantiateTypeWithIdentifierBridge1( getEntityIdentifierValue(
					typeDescriptor ) );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME )
					.add( getDocumentIdentifierValue(), b -> {} );
		}
		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projection_entityReference(PropertyTypeDescriptor<I, ?> typeDescriptor,
			DefaultIdentifierBridgeExpectations<I> expectations) {
		setup( typeDescriptor, expectations );
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
					.containsExactly( PojoEntityReference.withName(
							expectations.getTypeWithIdentifierBridge1(),
							DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME,
							getEntityIdentifierValue( typeDescriptor )
					) );
			backendMock.verifyExpectationsMet();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projection_id(PropertyTypeDescriptor<I, ?> typeDescriptor, DefaultIdentifierBridgeExpectations<I> expectations) {
		setup( typeDescriptor, expectations );
		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchIds(
					Collections.singletonList( DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME ),
					b -> {},
					StubSearchWorkBehavior.of(
							1L,
							getDocumentIdentifierValue()
					)
			);

			SearchQuery<Object> query = session.search( expectations.getTypeWithIdentifierBridge1() )
					.select( f -> f.id() )
					.where( f -> f.matchAll() )
					.toQuery();

			assertThat( query.fetchAll().hits() )
					.containsExactly( getEntityIdentifierValue( typeDescriptor ) );
			backendMock.verifyExpectationsMet();
		}
	}

	// Test behavior that backends expect from our bridges when using the DSLs
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void dslToIndexConverter(PropertyTypeDescriptor<I, ?> typeDescriptor, DefaultIdentifierBridgeExpectations<I> expectations) {
		setup( typeDescriptor, expectations );
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		DslConverter<I, ?> dslConverter =
				(DslConverter<I, ?>) indexModel.identifier().dslConverter();
		ToDocumentValueConvertContext convertContext =
				new ToDocumentValueConvertContextImpl( BridgeTestUtils.toBackendMappingContext( mapping ) );

		// The overriden default bridge must be used by the DSL converter
		assertThat( dslConverter.toDocumentValue( getEntityIdentifierValue( typeDescriptor ), convertContext ) )
				.isEqualTo( getDocumentIdentifierValue() );
		assertThat( dslConverter.unknownTypeToDocumentValue( getEntityIdentifierValue( typeDescriptor ), convertContext ) )
				.isEqualTo( getDocumentIdentifierValue() );
	}

	private I getEntityIdentifierValue(PropertyTypeDescriptor<I, ?> typeDescriptor) {
		return typeDescriptor.values().entityModelValues.get( 0 );
	}

	private String getDocumentIdentifierValue() {
		// See OverridingDefaultBridge
		return "OVERRIDDEN_ID";
	}

	private class OverridingDefaultBridge implements IdentifierBridge<I> {
		private static final String DOCUMENT_ID = "OVERRIDDEN_ID";
		private final PropertyTypeDescriptor<I, ?> typeDescriptor;

		private OverridingDefaultBridge(PropertyTypeDescriptor<I, ?> typeDescriptor) {
			this.typeDescriptor = typeDescriptor;
		}

		@Override
		public I fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			// We only support one ID value: this is just a stub
			return getEntityIdentifierValue( typeDescriptor );
		}

		@Override
		public String toDocumentIdentifier(I propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			// We only support one ID value: this is just a stub
			return DOCUMENT_ID;
		}
	}
}
