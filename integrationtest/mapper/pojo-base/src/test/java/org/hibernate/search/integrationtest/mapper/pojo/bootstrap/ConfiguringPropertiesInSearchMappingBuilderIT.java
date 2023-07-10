/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;

public class ConfiguringPropertiesInSearchMappingBuilderIT {

	@Rule
	public BackendMock backend = new BackendMock();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void propertyFileOverridden() {
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
					(StandalonePojoMappingConfigurer) context -> context.addEntityType( IndexedEntity.class )
			);
		}, IndexedEntity.INDEX, IndexedEntity.class );
	}

	@Test
	public void propertyFileOverrides() {
		test( b -> {
			// this should get overridden by the config value from the property file:
			b.property(
					StandalonePojoMapperSettings.MAPPING_CONFIGURER,
					(StandalonePojoMappingConfigurer) context -> context.addEntityType( IndexedEntity.class )
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
		SearchMappingBuilder builder = SearchMapping.builder( MethodHandles.lookup() )
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
			context.addEntityType( IndexedEntityFromFile.class );
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
