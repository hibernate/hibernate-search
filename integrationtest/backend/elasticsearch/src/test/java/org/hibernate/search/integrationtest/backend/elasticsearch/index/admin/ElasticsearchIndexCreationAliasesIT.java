/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.admin;

import static org.hibernate.search.integrationtest.backend.elasticsearch.index.admin.ElasticsearchAdminTestUtils.simpleReadAliasDefinition;
import static org.hibernate.search.integrationtest.backend.elasticsearch.index.admin.ElasticsearchAdminTestUtils.simpleWriteAliasDefinition;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.index.naming.IndexNamingStrategy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.StubSingleIndexNamingStrategy;
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

	private static final String BACKEND_NAME = "myElasticsearchBackend";
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
	public void success_defaultNamingStrategy() {
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
	public void success_customNamingStrategy() {
		elasticsearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setup( new StubSingleIndexNamingStrategy( "custom-write", "custom-read" ) );

		assertJsonEquals(
				"{"
						+ "'custom-write': " + simpleWriteAliasDefinition() + ", "
						+ "'custom-read': " + simpleReadAliasDefinition()
				+ "}",
				elasticsearchClient.index( INDEX_NAME ).aliases().get()
		);
	}

	private void setup(IndexNamingStrategy namingStrategy) {
		startSetupWithLifecycleStrategy()
				.withBackendProperty( BACKEND_NAME, ElasticsearchBackendSettings.NAMING_STRATEGY, namingStrategy )
				.withIndex( INDEX_NAME, ctx -> { } )
				.setup();
	}

	private SearchSetupHelper.SetupContext startSetupWithLifecycleStrategy() {
		return setupHelper.start( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						strategy.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				);
	}

}
