/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleReadAliasDefinition;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleWriteAliasDefinition;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.StubSingleIndexLayoutStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests related to aliases when creating indexes,
 * for all index-creating schema management operations.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchIndexSchemaManagerCreationAliasesIT {

	@Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creating();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	private final ElasticsearchIndexSchemaManagerOperation operation;

	public ElasticsearchIndexSchemaManagerCreationAliasesIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_defaultLayoutStrategy() {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( null );

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( index.name() ) + "': " + simpleWriteAliasDefinition() + ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + simpleReadAliasDefinition()
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	@Test
	public void success_noAliasLayoutStrategy() {
		elasticsearchClient.indexNoAlias( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( "no-alias" );

		assertJsonEquals(
				"{"
						+ "}",
				elasticsearchClient.indexNoAlias( index.name() ).aliases().get()
		);
	}

	@Test
	public void success_customLayoutStrategy() {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( new StubSingleIndexLayoutStrategy( "custom-write", "custom-read" ) );

		assertJsonEquals(
				"{"
						+ "'custom-write': " + simpleWriteAliasDefinition() + ", "
						+ "  'custom-read': " + simpleReadAliasDefinition()
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	/**
	 * Test that migrating from 6.0.0.Beta4 or earlier will just create new (empty) indexes,
	 * keeping the old ones in place.
	 */
	@Test
	public void migrationFrom6Beta4OrEarlier() {
		// Index layout of 6.0.0.Beta4 and before: aliases are missing,
		// and the primary Elasticsearch index name is just the Hibernate Search index name.
		URLEncodedString oldIndexName = encodeName( index.name() );
		elasticsearchClient.index( oldIndexName, null, null )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );

		setupAndCreateIndex( null );

		// New indexes are created
		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( index.name() ) + "': " + simpleWriteAliasDefinition() + ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + simpleReadAliasDefinition()
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
		// Old indexes are still there: we expect users to reindex and delete old indexes.
		assertJsonEquals(
				"{}",
				elasticsearchClient.index( oldIndexName, null, null ).aliases().get()
		);
	}

	private void setupAndCreateIndex(Object layoutStrategy) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withBackendProperty( ElasticsearchBackendSettings.LAYOUT_STRATEGY, layoutStrategy )
				.withIndex( index )
				.setup();

		operation.apply( index.schemaManager() ).join();
	}

}
