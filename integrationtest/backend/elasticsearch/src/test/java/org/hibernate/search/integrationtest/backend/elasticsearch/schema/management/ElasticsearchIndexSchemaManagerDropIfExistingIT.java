/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests related to dropping the index.
 */
class ElasticsearchIndexSchemaManagerDropIfExistingIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> root.field( "field", f -> f.asString() )
			.toReference()
	);

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	void alreadyExists() {
		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'field': {"
								+ "  'type': 'keyword'"
								+ "},"
								+ "'NOTmyField': {"
								+ "  'type': 'date'"
								+ "}"
				)
		);

		assertThat( elasticSearchClient.index( index.name() ).exists() ).isTrue();

		setupAndDropIndexIfExisting();

		assertThat( elasticSearchClient.index( index.name() ).exists() ).isFalse();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	void doesNotExist() {
		elasticSearchClient.index( index.name() ).ensureDoesNotExist();

		assertThat( elasticSearchClient.index( index.name() ).exists() ).isFalse();

		setupAndDropIndexIfExisting();

		// Nothing should have happened, and we should get here without throwing an exception
		assertThat( elasticSearchClient.index( index.name() ).exists() ).isFalse();
	}

	private void setupAndDropIndexIfExisting() {
		setupHelper.start()
				.withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.setup();

		Futures.unwrappedExceptionJoin( index.schemaManager().dropIfExisting( OperationSubmitter.blocking() ) );
	}

}
