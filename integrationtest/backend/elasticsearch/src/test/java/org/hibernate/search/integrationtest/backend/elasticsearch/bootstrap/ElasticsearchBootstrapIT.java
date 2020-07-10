/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchBootstrapIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy elasticsearchClientSpy = new ElasticsearchClientSpy();

	/**
	 * Check that we boot successfully when the Elasticsearch model dialect is determined
	 * from an explicitly configured Elasticsearch version,
	 * and that the Elasticsearch client starts in the second phase of bootstrap in that case.
	 */
	@Test
	public void explicitModelDialect() {
		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION, ElasticsearchTestDialect.getClusterVersion()
				)
				.withBackendProperty(
						ElasticsearchBackendSpiSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.factoryReference()
				)
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( StubMappedIndex.withoutFields() )
				.setupFirstPhaseOnly();

		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isEqualTo( 0 );
		elasticsearchClientSpy.verifyExpectationsMet();

		// In the *second* phase, however, we expect the client to be created and used to check the version
		elasticsearchClientSpy.expectNext(
				ElasticsearchRequest.get().build(), ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		partialSetup.doSecondPhase();
		elasticsearchClientSpy.verifyExpectationsMet();
	}

	/**
	 * Check that an exception is thrown when version check is at false without explicit cluster version specified
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3841")
	public void explicitProtocolDialect_noVersionCheck() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start()
						.withBackendProperty(
								ElasticsearchBackendSettings.VERSION_CHECK_ENABLED, false
						)
						.withBackendProperty(
								ElasticsearchBackendSpiSettings.CLIENT_FACTORY,
								elasticsearchClientSpy.factoryReference()
						)
						.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
						.withIndex( StubMappedIndex.withoutFields() )
						.setupFirstPhaseOnly(),
				"NO version check without explicit version number"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								"Invalid Elasticsearch version",
								"When version_check.enabled is set to false",
								"the version must at least be in the form 'x.y'"
						)
						.build()
				);
	}

	/**
	 * Check that an exception is thrown when version check is at false with a partial version is specified
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3841")
	public void explicitProtocolDialect_noVersionCheck_incompleteVersion() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start()
						.withBackendProperty(
								ElasticsearchBackendSettings.VERSION_CHECK_ENABLED, false
						)
						.withBackendProperty(
								ElasticsearchBackendSettings.VERSION, "7"
						)
						.withBackendProperty(
								ElasticsearchBackendSpiSettings.CLIENT_FACTORY,
								elasticsearchClientSpy.factoryReference()
						)
						.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
						.withIndex( StubMappedIndex.withoutFields() )
						.setupFirstPhaseOnly(),
				"NO version check with partial version number"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								"Invalid Elasticsearch version",
								"When version_check.enabled is set to false",
								"the version must at least be in the form 'x.y'"
						)
						.build()
				);
	}

	/**
	 * Check that everything is fine when version check is at false with a version is specified with major & minor
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3841")
	public void explicitProtocolDialect_noVersionCheck_completeVersion() {
		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION_CHECK_ENABLED, false
				)
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION, "7.8"
				)
				.withBackendProperty(
						ElasticsearchBackendSpiSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.factoryReference()
				)
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( StubMappedIndex.withoutFields() )
				.setupFirstPhaseOnly();
		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isEqualTo( 0 );
		elasticsearchClientSpy.verifyExpectationsMet();

		partialSetup.doSecondPhase();
		elasticsearchClientSpy.verifyExpectationsMet();
	}
}
