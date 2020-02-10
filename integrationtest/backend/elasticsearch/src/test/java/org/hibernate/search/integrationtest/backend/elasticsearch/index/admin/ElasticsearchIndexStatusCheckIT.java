/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.admin;

import static org.hibernate.search.integrationtest.backend.elasticsearch.index.admin.ElasticsearchAdminTestUtils.simpleMappingForInitialization;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the index status checks,
 * for all applicable index lifecycle strategies.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-2456")
public class ElasticsearchIndexStatusCheckIT {

	private static final String INDEX_NAME = "IndexName";

	@Parameters(name = "With strategy {0}")
	public static EnumSet<IndexLifecycleStrategyName> strategies() {
		// The "NONE" strategy never checks that the index exists.
		return EnumSet.complementOf( EnumSet.of( IndexLifecycleStrategyName.NONE ) );
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private IndexLifecycleStrategyName strategy;

	public ElasticsearchIndexStatusCheckIT(IndexLifecycleStrategyName strategy) {
		super();
		this.strategy = strategy;
	}

	@Test
	public void indexMissing() throws Exception {
		Assume.assumeFalse( "The strategy " + strategy + " creates an index automatically."
				+ " No point running this test.",
				createsIndex( strategy ) );

		elasticSearchClient.index( INDEX_NAME ).ensureDoesNotExist();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.multilineFailure(
								"HSEARCH400050"
						)
						.build()
		);
	}

	@Test
	public void invalidIndexStatus_creatingIndex() throws Exception {
		Assume.assumeTrue( "The strategy " + strategy + " doesn't creates an index automatically."
				+ " No point running this test.",
				createsIndex( strategy ) );

		// Make sure automatically created indexes will never be green
		elasticSearchClient.template( "yellow_index_because_not_enough_nodes_for_so_many_replicas" )
				.create(
						"*",
						/*
						 * The exact number of replicas we ask for doesn't matter much,
						 * since we're testing with only 1 node (the cluster can't replicate shards)
						 */
						"{'number_of_replicas': 5}"
				);

		elasticSearchClient.index( INDEX_NAME ).ensureDoesNotExist();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.multilineFailure(
								"HSEARCH400024",
								"100ms"
						)
						.build()
		);
	}

	@Test
	public void invalidIndexStatus_usingPreexistingIndex() throws Exception {
		// Make sure automatically created indexes will never be green
		elasticSearchClient.template( "yellow_index_because_not_enough_nodes_for_so_many_replicas" )
				.create(
						"*",
						/*
						 * The exact number of replicas we ask for doesn't matter much,
						 * since we're testing with only 1 node (the cluster can't replicate shards)
						 */
						"{'number_of_replicas': 5}"
				);

		elasticSearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping(
						simpleMappingForInitialization( "" )
				);

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.multilineFailure(
								"HSEARCH400024",
								"100ms"
						)
						.build()
		);
	}

	private void setupExpectingFailure(String failureReportRegex) {
		SubTest.expectException( this::setup )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( failureReportRegex );
	}

	private void setup() {
		startSetupWithLifecycleStrategy()
				.withIndex(
						INDEX_NAME,
						ctx -> { }
				)
				.setup();
	}

	private SearchSetupHelper.SetupContext startSetupWithLifecycleStrategy() {
		return setupHelper.start()
				.withBackendProperty(
						// Don't contribute any analysis definitions, validation of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						strategy.getExternalRepresentation()
				)
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS,
						IndexStatus.GREEN.getElasticsearchString()
				)
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT,
						"100"
				);
	}

	private boolean createsIndex(IndexLifecycleStrategyName strategy) {
		return !IndexLifecycleStrategyName.NONE.equals( strategy )
				&& !IndexLifecycleStrategyName.VALIDATE.equals( strategy );
	}

}
