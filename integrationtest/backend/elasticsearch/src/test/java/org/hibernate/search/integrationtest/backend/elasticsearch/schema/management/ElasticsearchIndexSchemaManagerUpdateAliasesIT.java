/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories.RequiresIndexAliasIsWriteIndex;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests related to aliases when updating indexes.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchIndexSchemaManagerUpdateAliasesIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private StubMappingIndexManager indexManager;

	@After
	public void cleanUp() {
		if ( indexManager != null ) {
			indexManager.getSchemaManager().dropIfExisting();
		}
	}

	@Test
	public void nothingToDo() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleWriteAliasDefinition() + ", "
						+ "'" + defaultReadAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleReadAliasDefinition() + ", "
						+ "'somePreExistingAlias': {"
						+ "}"
				+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
	}

	@Test
	public void writeAlias_missing() {
		elasticsearchClient.index( defaultPrimaryName( INDEX_NAME ), null, defaultReadAlias( INDEX_NAME ) )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleWriteAliasDefinition() + ", "
						+ "'" + defaultReadAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleReadAliasDefinition() + ", "
						+ "'somePreExistingAlias': {"
						+ "}"
				+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
	}

	@Test
	public void writeAlias_invalid_filter() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( defaultWriteAlias( INDEX_NAME ).original, "{'filter': {'term': {'user_id': 12}}}" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleWriteAliasDefinition() + ", "
						+ "'" + defaultReadAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleReadAliasDefinition() + ", "
						+ "'somePreExistingAlias': {}"
						+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
	}

	@Test
	@Category(RequiresIndexAliasIsWriteIndex.class)
	public void writeAlias_invalid_isWriteIndex() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( defaultWriteAlias( INDEX_NAME ).original, ElasticsearchIndexSchemaManagerTestUtils
						.simpleAliasDefinition( false, "" ) );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleWriteAliasDefinition() + ", "
						+ "'" + defaultReadAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleReadAliasDefinition() + ", "
						+ "'somePreExistingAlias': {}"
				+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
	}

	@Test
	public void readAlias_missing() {
		elasticsearchClient.index( defaultPrimaryName( INDEX_NAME ), defaultWriteAlias( INDEX_NAME ), null )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleWriteAliasDefinition() + ", "
						+ "'" + defaultReadAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleReadAliasDefinition() + ", "
						+ "'somePreExistingAlias': {}"
				+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
	}

	@Test
	public void readAlias_invalid_filter() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( defaultReadAlias( INDEX_NAME ).original, "{'filter': {'term': {'user_id': 12}}}" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleWriteAliasDefinition() + ", "
						+ "'" + defaultReadAlias( INDEX_NAME ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
						.simpleReadAliasDefinition() + ", "
						+ "'somePreExistingAlias': {}"
				+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
	}

	private void setupAndUpdateIndex() {
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
				.withIndex( INDEX_NAME, ctx -> { }, indexManager -> this.indexManager = indexManager )
				.setup();

		indexManager.getSchemaManager().createOrUpdate().join();
	}

}
