/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the index status checks,
 * for all status-checking schema management operations.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-2456")
public class ElasticsearchIndexSchemaManagerStatusCheckIT {

	@Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.statusChecking();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	private final ElasticsearchIndexSchemaManagerOperation operation;

	public ElasticsearchIndexSchemaManagerStatusCheckIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Before
	public void checkAssumptions() {
		assumeTrue(
				"This test only makes sense if the backend supports index status checks",
				ElasticsearchTckBackendFeatures.supportsIndexStatusCheck()
		);
	}

	@Test
	public void indexMissing() throws Exception {
		assumeFalse( "The operation " + operation + " creates an index automatically."
				+ " No point running this test.",
				ElasticsearchIndexSchemaManagerOperation.creating().contains( operation ) );

		elasticSearchClient.index( index.name() ).ensureDoesNotExist();

		assertThatThrownBy( () -> setupAndInspectIndex( "index-settings-for-tests/5-replicas.json" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "HSEARCH400050" );
	}

	@Test
	public void invalidIndexStatus_creatingIndex() throws Exception {
		assumeTrue( "The operation " + operation + " doesn't create an index automatically."
				+ " No point running this test.",
				ElasticsearchIndexSchemaManagerOperation.creating().contains( operation ) );

		elasticSearchClient.index( index.name() ).ensureDoesNotExist();

		// Make sure automatically created indexes will never be green by requiring 5 replicas
		// (more than the amount of ES nodes)
		assertThatThrownBy( () -> setupAndInspectIndex( "index-settings-for-tests/5-replicas.json" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "HSEARCH400024", "100ms" );
	}

	@Test
	public void invalidIndexStatus_usingPreexistingIndex() throws Exception {
		assumeFalse( "The operation " + operation + " drops the existing index automatically."
				+ " No point running this test.",
				ElasticsearchIndexSchemaManagerOperation.dropping().contains( operation ) );

		// Make sure automatically created indexes will never be green by requiring 5 replicas
		// (more than the amount of ES nodes)
		elasticSearchClient.index( index.name() )
				.deleteAndCreate( "number_of_replicas", "5" )
				.type().putMapping(
						simpleMappingForInitialization( "" )
				);

		assertThatThrownBy( () -> setupAndInspectIndex( "index-settings-for-tests/5-replicas.json" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "HSEARCH400024", "100ms" );
	}


	private void setupAndInspectIndex(String settingsPath) {
		setupHelper.start()
				.withBackendProperty(
						// Don't contribute any analysis definitions, validation of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE, settingsPath )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS,
						IndexStatus.GREEN.externalRepresentation()
				)
				.withBackendProperty(
						ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT,
						"100"
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}

}
