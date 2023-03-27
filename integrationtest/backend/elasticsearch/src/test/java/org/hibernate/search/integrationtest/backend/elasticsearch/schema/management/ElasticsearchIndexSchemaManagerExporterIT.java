/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect.isActualVersion;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaCollector;
import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ElasticsearchIndexSchemaManagerExporterIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final StubMappedIndex mainIndex = StubMappedIndex.withoutFields().name( "main" );

	@Before
	public void setUp() {
		setupHelper.start()
				.withBackendProperty(
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						// use this configurer to produce a huge config:
						new ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer()
				)
				.withIndex( mainIndex )
				.setup();
	}

	@Test
	public void export() throws IOException {
		assumeFalse(
				"Older versions of Elasticsearch would not match the mappings",
				isActualVersion(
						esVersion -> esVersion.isLessThan( "7.0" ),
						osVersion -> false
				)
		);
		Path directory = temporaryFolder.newFolder().toPath();
		String testIndexName = "test";
		mainIndex.schemaManager().exportExpectedSchema( new IndexSchemaCollector() {
			@Override
			public void indexSchema(Optional<String> backendName, String indexName, SchemaExport export) {
				export.toFiles( directory.resolve( testIndexName ) );
			}
		} );

		assertJsonEqualsIgnoringUnknownFields(
				"{" +
						"  \"mappings\": {" +
						"    \"properties\": {" +
						"      \"_entity_type\": {" +
						"        \"type\": \"keyword\"" +
						"      }" +
						"    }" +
						"  }" +
						"}",
				readString( directory.resolve( testIndexName ).resolve( "create-index.json" ) )
		);

		assertJsonEquals(
				"{}",
				readString( directory.resolve( testIndexName ).resolve( "create-index-query-params.json" ) )
		);
	}

	private String readString(Path path) throws IOException {
		try ( Stream<String> lines = Files.lines( path ) ) {
			return lines.collect( Collectors.joining( "\n" ) );
		}
	}
}
