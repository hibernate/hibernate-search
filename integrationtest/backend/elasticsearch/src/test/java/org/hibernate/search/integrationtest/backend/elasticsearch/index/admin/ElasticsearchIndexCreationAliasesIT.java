/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.admin;

import static org.hibernate.search.integrationtest.backend.elasticsearch.index.admin.ElasticsearchAdminTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.index.admin.ElasticsearchAdminTestUtils.simpleReadAliasDefinition;
import static org.hibernate.search.integrationtest.backend.elasticsearch.index.admin.ElasticsearchAdminTestUtils.simpleWriteAliasDefinition;
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
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests related to aliases when creating indexes,
 * for all applicable index lifecycle strategies.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchIndexCreationAliasesIT {

	private static final String INDEX_NAME = "IndexName";

	@Parameters(name = "With strategy {0}")
	public static EnumSet<IndexLifecycleStrategyName> strategies() {
		return EnumSet.complementOf( EnumSet.of(
				// These strategies don't create the indexes, so we don't test them.
				IndexLifecycleStrategyName.NONE, IndexLifecycleStrategyName.VALIDATE
		) );
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final IndexLifecycleStrategyName strategy;

	public ElasticsearchIndexCreationAliasesIT(IndexLifecycleStrategyName strategy) {
		this.strategy = strategy;
	}

	@Test
	public void success_defaultLayoutStrategy() {
		elasticsearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setup( null );

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

		setup( new StubSingleIndexLayoutStrategy( "custom-write", "custom-read" ) );

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

		setup( null );

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

	private void setup(IndexLayoutStrategy layoutStrategy) {
		startSetupWithLifecycleStrategy()
				.withBackendProperty( ElasticsearchBackendSettings.LAYOUT_STRATEGY, layoutStrategy )
				.withIndex( INDEX_NAME, ctx -> { } )
				.setup();
	}

	private SearchSetupHelper.SetupContext startSetupWithLifecycleStrategy() {
		return setupHelper.start()
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						strategy.getExternalRepresentation()
				)
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				);
	}

}
