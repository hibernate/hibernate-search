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
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to aliases when updating indexes.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchIndexSchemaManagerUpdateAliasesIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@Test
	public void nothingToDo_defaultLayoutStrategy() {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "  '" + defaultWriteAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleWriteAliasDefinition()
						+ ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleReadAliasDefinition()
						+ ", "
						+ "  'somePreExistingAlias': {"
						+ "  }"
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	@Test
	public void nothingToDo_noAliasLayoutStrategy() {
		elasticsearchClient.indexNoAlias( index.name() )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.indexNoAlias( index.name() ).aliases()
				.put( "somePreExistingAlias" );

		setupAndUpdateIndex( "no-alias" );

		assertJsonEquals(
				"{"
						+ "  'somePreExistingAlias': {"
						+ "  }"
						+ "}",
				elasticsearchClient.indexNoAlias( index.name() ).aliases().get()
		);
	}

	@Test
	public void writeAlias_missing() {
		elasticsearchClient.index( defaultPrimaryName( index.name() ), null, defaultReadAlias( index.name() ) )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "  '" + defaultWriteAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleWriteAliasDefinition()
						+ ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleReadAliasDefinition()
						+ ", "
						+ "  'somePreExistingAlias': {"
						+ "  }"
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	@Test
	public void writeAlias_invalid_filter() {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( index.name() ).aliases()
				.put( defaultWriteAlias( index.name() ).original, "{'filter': {'term': {'user_id': 12}}}" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "  '" + defaultWriteAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleWriteAliasDefinition()
						+ ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleReadAliasDefinition()
						+ ", "
						+ "  'somePreExistingAlias': {}"
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	@Test
	public void writeAlias_invalid_isWriteIndex() {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( index.name() ).aliases()
				.put( defaultWriteAlias( index.name() ).original, ElasticsearchIndexSchemaManagerTestUtils
						.simpleAliasDefinition( false, "" ) );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "  '" + defaultWriteAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleWriteAliasDefinition()
						+ ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleReadAliasDefinition()
						+ ", "
						+ "  'somePreExistingAlias': {}"
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	@Test
	public void readAlias_missing() {
		elasticsearchClient.index( defaultPrimaryName( index.name() ), defaultWriteAlias( index.name() ), null )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "  '" + defaultWriteAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleWriteAliasDefinition()
						+ ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleReadAliasDefinition()
						+ ", "
						+ "  'somePreExistingAlias': {}"
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	@Test
	public void readAlias_invalid_filter() {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping( ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( index.name() ).aliases()
				.put( defaultReadAlias( index.name() ).original, "{'filter': {'term': {'user_id': 12}}}" );

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ "  '" + defaultWriteAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleWriteAliasDefinition()
						+ ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + ElasticsearchIndexSchemaManagerTestUtils
								.simpleReadAliasDefinition()
						+ ", "
						+ "  'somePreExistingAlias': {}"
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	private void setupAndUpdateIndex() {
		setupAndUpdateIndex( null );
	}

	private void setupAndUpdateIndex(Object layoutStrategy) {
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

		index.schemaManager().createOrUpdate( OperationSubmitter.blocking() ).join();
	}

}
