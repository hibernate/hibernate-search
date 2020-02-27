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
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.StubSingleIndexLayoutStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.After;
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

	private static final String INDEX_NAME = "IndexName";

	@Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creating();
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final ElasticsearchIndexSchemaManagerOperation operation;

	private StubMappingIndexManager indexManager;

	public ElasticsearchIndexSchemaManagerCreationAliasesIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@After
	public void cleanUp() {
		if ( indexManager != null ) {
			indexManager.getSchemaManager().dropIfExisting();
		}
	}

	@Test
	public void success_defaultLayoutStrategy() {
		elasticsearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( null );

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( INDEX_NAME ) + "': " + simpleWriteAliasDefinition() + ", "
						+ "'" + defaultReadAlias( INDEX_NAME ) + "': " + simpleReadAliasDefinition()
				+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
	}

	@Test
	public void success_customLayoutStrategy() {
		elasticsearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( new StubSingleIndexLayoutStrategy( "custom-write", "custom-read" ) );

		assertJsonEquals(
				"{"
						+ "'custom-write': " + simpleWriteAliasDefinition() + ", "
						+ "'custom-read': " + simpleReadAliasDefinition()
				+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
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
		URLEncodedString oldIndexName = encodeName( INDEX_NAME );
		elasticsearchClient.index( oldIndexName, null, null )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );

		setupAndCreateIndex( null );

		// New indexes are created
		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( INDEX_NAME ) + "': " + simpleWriteAliasDefinition() + ", "
						+ "'" + defaultReadAlias( INDEX_NAME ) + "': " + simpleReadAliasDefinition()
						+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
		// Old indexes are still there: we expect users to reindex and delete old indexes.
		assertJsonEquals(
				"{}",
				elasticsearchClient.index( oldIndexName, null, null ).aliases().get()
		);
	}

	private void setupAndCreateIndex(IndexLayoutStrategy layoutStrategy) {
		setupHelper.start()
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.NONE
				)
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withBackendProperty( ElasticsearchBackendSettings.LAYOUT_STRATEGY, layoutStrategy )
				.withIndex( INDEX_NAME, ctx -> { }, indexManager -> this.indexManager = indexManager )
				.setup();

		operation.apply( indexManager.getSchemaManager() ).join();
	}

}
