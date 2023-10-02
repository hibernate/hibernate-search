/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.cfg.spi.StandalonePojoMapperSpiSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMappingBuilder;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.util.impl.integrationtest.common.bean.ForbiddenBeanProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

class ConfiguringPropertiesInSearchMappingBuilderIT {

	@RegisterExtension
	public BackendMock backend = BackendMock.create();

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	void propertyFileOverridden() {
		test( b -> {
			try ( Reader properties = new InputStreamReader(
					getClass().getClassLoader().getResourceAsStream( "standalone-test.properties" ),
					StandardCharsets.UTF_8
			) ) {
				b.properties( properties );
			}
			catch (IOException e) {
				fail( e.getMessage() );
			}
			// Override the config value from the property file:
			b.property(
					StandalonePojoMapperSettings.MAPPING_CONFIGURER,
					(StandalonePojoMappingConfigurer) context -> context.programmaticMapping()
							.type( IndexedEntity.class )
							.searchEntity()
			);
		}, IndexedEntity.INDEX, IndexedEntity.class );
	}

	@Test
	void propertyFileOverrides() {
		test( b -> {
			// this should get overridden by the config value from the property file:
			b.property(
					StandalonePojoMapperSettings.MAPPING_CONFIGURER,
					(StandalonePojoMappingConfigurer) context -> context.programmaticMapping()
							.type( IndexedEntity.class )
							.searchEntity()
			);
			try ( Reader properties = new InputStreamReader(
					getClass().getClassLoader().getResourceAsStream( "standalone-test.properties" ),
					StandardCharsets.UTF_8
			) ) {
				b.properties( properties );
			}
			catch (IOException e) {
				fail( e.getMessage() );
			}
		}, IndexedEntityFromFile.INDEX, IndexedEntityFromFile.class );
	}

	private void test(Consumer<SearchMappingBuilder> configurer, String index, Class<?> type) {
		logged.expectEvent( Level.WARN, "Invalid configuration passed to Hibernate Search",
				"some properties in the given configuration are not used",
				"hibernate.search.something.unused",
				"hibernate.search.programmatic.something.unused",
				"hibernate.search.programmatic.after.something.unused"
		);
		logged.expectEvent( Level.WARN, "Invalid configuration passed to Hibernate Search",
				"some properties in the given configuration are not used",
				"hibernate.search.programmatic.something.unused"
		);
		SearchMappingBuilder builder = SearchMapping.builder( AnnotatedTypeSource.empty(), MethodHandles.lookup() )
				.property( StandalonePojoMapperSpiSettings.BEAN_PROVIDER, new ForbiddenBeanProvider() )
				.property( "hibernate.search.programmatic.something.unused", false );
		configurer.accept( builder );
		builder.property(
				EngineSettings.BACKEND + ".type",
				backend.factory( CompletableFuture.completedStage( null ) )
		);
		builder.property( "hibernate.search.programmatic.after.something.unused", false );
		backend.expectSchema( index, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ).projectable( Projectable.YES ) )
		).expectSchemaManagementWorks( index )
				.work( StubSchemaManagementWork.Type.CREATE_OR_VALIDATE );

		// we want to check that the configurer property from the file got overridden
		// by the one provided through properties on a builder:
		try ( CloseableSearchMapping searchMapping = builder.build() ) {
			assertThat( searchMapping.allIndexedEntities() )
					.hasSize( 1 );
			assertThat( searchMapping.allIndexedEntities().iterator().next().javaClass() )
					.isEqualTo( type );
		}
	}

	public static class FileStandalonePojoMappingConfigurer implements StandalonePojoMappingConfigurer {
		@Override
		public void configure(StandalonePojoMappingConfigurationContext context) {
			context.programmaticMapping().type( IndexedEntityFromFile.class ).searchEntity();
		}
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {
		public static final String INDEX = "IndexedEntity";
		@DocumentId
		public String id;
		@FullTextField(projectable = Projectable.YES)
		public String text;

		public IndexedEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Indexed(index = IndexedEntityFromFile.INDEX)
	public static class IndexedEntityFromFile {
		public static final String INDEX = "IndexedEntityFromFile";
		@DocumentId
		public String id;
		@FullTextField(projectable = Projectable.YES)
		public String text;

		public IndexedEntityFromFile(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
