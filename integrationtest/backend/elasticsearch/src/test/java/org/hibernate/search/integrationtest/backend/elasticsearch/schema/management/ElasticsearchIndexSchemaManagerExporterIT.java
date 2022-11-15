/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;

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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class ElasticsearchIndexSchemaManagerExporterIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();
	@TempDir
	Path temporaryFolder;

	private final StubMappedIndex mainIndex = StubMappedIndex.withoutFields().name( "main" );

	@BeforeEach
	void setUp() {
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
	void export() throws IOException {
		Path directory = temporaryFolder;
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
