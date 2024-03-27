/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.providedid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.integrationtest.mapper.pojo.mapping.definition.BridgeTestUtils;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProvidedIdIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private StubIndexModel indexModel;

	@Test
	void converters() {
		final String entityAndIndexName = "indexed";
		@SearchEntity(name = entityAndIndexName)
		@Indexed
		class IndexedEntity {
		}

		// Schema
		backendMock.expectSchema( entityAndIndexName, b -> {},
				indexModel -> this.indexModel = indexModel );
		SearchMapping mapping = withBaseConfiguration()
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
		backendMock.verifyExpectationsMet();

		// DslConverter
		@SuppressWarnings("unchecked")
		DslConverter<Object, String> dslConverter =
				(DslConverter<Object, String>) indexModel.identifier().dslConverter();
		ToDocumentValueConvertContext toDocumentConvertContext =
				new ToDocumentValueConvertContextImpl( BridgeTestUtils.toBackendMappingContext( mapping ) );
		assertThat( dslConverter.toDocumentValue( 120, toDocumentConvertContext ) )
				.isEqualTo( "120" );
		assertThat( dslConverter.unknownTypeToDocumentValue( 120, toDocumentConvertContext ) )
				.isEqualTo( "120" );

		// ProjectionConverter
		@SuppressWarnings("unchecked")
		ProjectionConverter<String, Object> projectionConverter =
				(ProjectionConverter<String, Object>) indexModel.identifier().projectionConverter();
		try ( SearchSession searchSession = mapping.createSession() ) {
			FromDocumentValueConvertContext fromDocumentConvertContext =
					new FromDocumentValueConvertContextImpl( BridgeTestUtils.toBackendSessionContext( searchSession ) );
			assertThat( projectionConverter.fromDocumentValue( "120", fromDocumentConvertContext ) )
					.isEqualTo( 120 );
		}
	}

	@Test
	void indexAndSearch() {
		final String entityAndIndexName = "indexed";
		@SearchEntity(name = entityAndIndexName)
		@Indexed
		class IndexedEntity {
		}

		// Schema
		backendMock.expectSchema( entityAndIndexName, b -> {} );
		SearchMapping mapping = withBaseConfiguration()
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();

			session.indexingPlan().add( 42, null, entity1 );

			backendMock.expectWorks( entityAndIndexName )
					.add( "42", b -> {} );
		}
		backendMock.verifyExpectationsMet();

		// Searching
		try ( SearchSession session = mapping.createSession() ) {
			// Check provided ID bridge is applied when fetching entity references
			backendMock.expectSearchReferences(
					Collections.singletonList( entityAndIndexName ),
					StubSearchWorkBehavior.of(
							1L,
							StubBackendUtils.reference( entityAndIndexName, "42" )
					)
			);
			assertThat( session.search( IndexedEntity.class )
					.selectEntityReference()
					.where( f -> f.matchAll() ).fetchAllHits() )
					.containsExactly( PojoEntityReference.withName( IndexedEntity.class, entityAndIndexName, 42 ) );

			// Check provided ID bridge is applied when fetching IDs
			backendMock.expectSearchIds(
					Collections.singletonList( entityAndIndexName ),
					b -> {},
					StubSearchWorkBehavior.of(
							1L,
							Arrays.asList( "42" )
					)
			);
			assertThat( session.search( IndexedEntity.class )
					.select( f -> f.id() )
					.where( f -> f.matchAll() ).fetchAllHits() )
					.containsExactly( 42 );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void error_nullProvidedId() {
		final String entityAndIndexName = "indexed";
		@SearchEntity(name = entityAndIndexName)
		@Indexed
		class IndexedEntity {
		}

		backendMock.expectAnySchema( entityAndIndexName );
		SearchMapping mapping = withBaseConfiguration()
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();

			assertThatThrownBy(
					() -> session.indexingPlan().add( entity1 )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "The entity identifier must not be null" );
		}
	}

	private StandalonePojoMappingSetupHelper.SetupContext withBaseConfiguration() {
		return setupHelper.start()
				.withConfiguration(
						b -> b.providedIdentifierBridge( BeanReference.ofInstance( new NaiveIdentifierBridge() ) ) );
	}

	public static class NaiveIdentifierBridge implements IdentifierBridge<Object> {
		private Map<String, Object> objectIds = new HashMap<>();

		@Override
		public String toDocumentIdentifier(Object propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			String documentId = propertyValue.toString();
			objectIds.put( documentId, propertyValue );
			return documentId;
		}

		@Override
		public Object fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
			return objectIds.get( documentIdentifier );
		}
	}
}
