/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
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
						new ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer() // use this configurer to produce a huge config ;)
				)
				.withIndex( mainIndex )
				.setup();
	}

	@Test
	public void export() throws IOException {
		Path directory = temporaryFolder.newFolder().toPath();
		String testIndexName = "test name";
		mainIndex.schemaManager().exportSchema( directory, testIndexName );

		String index = Files.readString(
				directory.resolve( "backend" )
						.resolve( "indexes" )
						.resolve( testIndexName )
						.resolve( "index.json" )
		);
		assertJsonEquals(
				"{" +
						"  \"aliases\": {" +
						"    \"main-write\": {" +
						"      \"is_write_index\": true" +
						"    }," +
						"    \"main-read\": {" +
						"      \"is_write_index\": false" +
						"    }" +
						"  }," +
						"  \"mapping\": {" +
						"    \"properties\": {" +
						"      \"_entity_type\": {" +
						"        \"type\": \"keyword\"," +
						"        \"index\": false," +
						"        \"doc_values\": true" +
						"      }" +
						"    }," +
						"    \"dynamic\": \"strict\"" +
						"  }," +
						"  \"settings\": {" +
						"    \"analysis\": {" +
						"      \"analyzer\": {" +
						"        \"custom-analyzer\": {" +
						"          \"type\": \"custom\"," +
						"          \"tokenizer\": \"custom-edgeNGram\"," +
						"          \"filter\": [" +
						"            \"custom-keep-types\"," +
						"            \"custom-word-delimiter\"" +
						"          ]," +
						"          \"char_filter\": [" +
						"            \"custom-pattern-replace\"" +
						"          ]" +
						"        }" +
						"      }," +
						"      \"tokenizer\": {" +
						"        \"custom-edgeNGram\": {" +
						"          \"type\": \"edge_ngram\"," +
						"          \"min_gram\": 1," +
						"          \"max_gram\": 10" +
						"        }" +
						"      }," +
						"      \"filter\": {" +
						"        \"custom-keep-types\": {" +
						"          \"type\": \"keep_types\"," +
						"          \"types\": [" +
						"            \"\\u003cNUM\\u003e\"," +
						"            \"\\u003cDOUBLE\\u003e\"" +
						"          ]" +
						"        }," +
						"        \"custom-word-delimiter\": {" +
						"          \"type\": \"word_delimiter\"," +
						"          \"generate_word_parts\": false" +
						"        }" +
						"      }," +
						"      \"char_filter\": {" +
						"        \"custom-pattern-replace\": {" +
						"          \"type\": \"pattern_replace\"," +
						"          \"pattern\": \"[^0-9]\"," +
						"          \"replacement\": \"0\"," +
						"          \"tags\": \"CASE_INSENSITIVE|COMMENTS\"" +
						"        }" +
						"      }" +
						"    }" +
						"  }" +
						"}",
				index
		);
	}

	@Test
	public void exportToExistingDirectory() throws IOException {
		String testIndexName = "test name";
		Path directory = temporaryFolder.newFolder().toPath();
		Path path = Files.createDirectories( directory.resolve( "backend" )
				.resolve( "indexes" )
				.resolve( testIndexName )
		);
		Files.writeString(
				path
						.resolve( "not-an-index.json" ),
				"{}"
		);

		assertThatThrownBy( () -> mainIndex.schemaManager().exportSchema( directory, testIndexName ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Target path",
						path.toString(),
						"already exists and is not an empty directory",
						"Use a path to an empty or non-existing directory"
				);
	}

	@Test
	public void exportToExistingFile() throws IOException {
		String testIndexName = "test name";
		Path directory = temporaryFolder.newFolder().toPath();
		Path path = Files.createDirectories( directory.resolve( "backend" )
				.resolve( "indexes" )
		);
		Files.writeString(
				path.resolve( testIndexName ),
				"{}"
		);

		assertThatThrownBy( () -> mainIndex.schemaManager().exportSchema( directory, testIndexName ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Target path",
						path.resolve( testIndexName ).toString(),
						"already exists and is not an empty directory",
						"Use a path to an empty or non-existing directory"
				);
	}
}
