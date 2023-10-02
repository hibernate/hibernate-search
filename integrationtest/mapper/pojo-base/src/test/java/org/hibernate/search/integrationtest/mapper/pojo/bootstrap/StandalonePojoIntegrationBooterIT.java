/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooter;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingHandle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.Fail;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class StandalonePojoIntegrationBooterIT {

	private static final String INDEX_NAME = "IndexName";

	private final List<AutoCloseable> toClose = new ArrayList<>();

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@Mock
	private ValueReadHandle<Integer> idValueReadHandleMock;

	@Mock
	private ValueReadHandle<String> textValueReadHandleMock;

	@Mock
	private ValueHandleFactory valueHandleFactoryMock;

	@AfterEach
	void cleanup() throws Exception {
		try ( Closer<Exception> closer = new Closer<>() ) {
			closer.pushAll( AutoCloseable::close, toClose );
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void twoPhaseBoot() throws Exception {
		CompletableFuture<BackendMappingHandle> mappingHandlePromise = new CompletableFuture<>();
		StandalonePojoIntegrationBooter preBooter = StandalonePojoIntegrationBooter.builder()
				.valueReadHandleFactory( valueHandleFactoryMock )
				.property(
						EngineSettings.BACKEND + "." + BackendSettings.TYPE,
						backendMock.factory( mappingHandlePromise )
				)
				.property( StandalonePojoMapperSettings.MAPPING_CONFIGURER, new StandalonePojoMappingConfigurer() {
					@Override
					public void configure(StandalonePojoMappingConfigurationContext context) {
						context.annotationMapping().add( IndexedEntity.class );
					}
				} )
				.build();

		Map<String, Object> preBooterGeneratedProperties = new LinkedHashMap<>();

		// Pre-booting should lead to a schema definition in the backend.
		backendMock.expectSchema( INDEX_NAME, b -> b.field( "text", String.class,
				b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) ) );
		// Pre-booting should retrieve value-read handles
		// Simulate a custom handle from a framework, e.g. Quarkus
		when( valueHandleFactoryMock.createForField( IndexedEntity.ID_FIELD ) )
				.thenReturn( (ValueReadHandle) idValueReadHandleMock );
		when( valueHandleFactoryMock.createForField( IndexedEntity.TEXT_FIELD ) )
				.thenReturn( (ValueReadHandle) textValueReadHandleMock );
		preBooter.preBoot( preBooterGeneratedProperties::put );
		backendMock.verifyExpectationsMet();
		verify( valueHandleFactoryMock ).createForField( IndexedEntity.ID_FIELD );
		verify( valueHandleFactoryMock ).createForField( IndexedEntity.TEXT_FIELD );

		StandalonePojoIntegrationBooter actualBooter = StandalonePojoIntegrationBooter.builder()
				.properties( preBooterGeneratedProperties )
				/*
				 * We use a "trapped" mapping configurer to check that Hibernate Search does not generate the mapping,
				 * but re-uses the one generated by the "pre-boot" above.
				 */
				.property( StandalonePojoMapperSettings.MAPPING_CONFIGURER, new StandalonePojoMappingConfigurer() {
					@Override
					public void configure(StandalonePojoMappingConfigurationContext context) {
						Fail.fail( "Hibernate Search did not re-use the mapping generated when pre-booting" );
					}
				} )
				// We set a non-default schema management strategy in the second phase of booting
				// to check that Hibernate Search takes it into account then, not in the first phase.
				.property( StandalonePojoMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						SchemaManagementStrategyName.DROP_AND_CREATE )
				.build();

		// Actually booting the mapping should lead to a schema creation in the backend.
		backendMock.expectSchemaManagementWorks( INDEX_NAME )
				// And the schema management strategy should be the one set in the second phase of bootstrap.
				.work( StubSchemaManagementWork.Type.DROP_AND_CREATE );
		try ( CloseableSearchMapping mapping = actualBooter.boot() ) {
			mappingHandlePromise.complete( new StandalonePojoMappingHandle() );

			/*
			 * Building the session should NOT lead to a second schema creation in the backend:
			 * that would mean the pre-boot was ignored...
			 */
			backendMock.verifyExpectationsMet();

			try ( SearchSession session = mapping.createSession() ) {
				IndexedEntity entity = new IndexedEntity();
				entity.id = 1;
				entity.text = "someText";
				when( idValueReadHandleMock.get( entity ) ).thenReturn( entity.id );
				session.indexingPlan().add( entity );

				when( textValueReadHandleMock.get( entity ) ).thenReturn( entity.text );

				backendMock.expectWorks( INDEX_NAME )
						.add( "1", b -> b.field( "text", "someText" ) );
			}
			// If the entity was indexed, it means Hibernate Search booted correctly
			backendMock.verifyExpectationsMet();

			// Also check that the value handle has been called
			verify( textValueReadHandleMock ).get( any() );
		}
	}

	@SearchEntity
	@Indexed(index = INDEX_NAME)
	private static class IndexedEntity {
		private static final Field ID_FIELD;
		private static final Field TEXT_FIELD;

		static {
			try {
				ID_FIELD = IndexedEntity.class.getDeclaredField( "id" );
				TEXT_FIELD = IndexedEntity.class.getDeclaredField( "text" );
			}
			catch (NoSuchFieldException e) {
				throw new IllegalStateException( e );
			}
		}

		@DocumentId
		private Integer id;
		@FullTextField
		private String text;
	}
}
