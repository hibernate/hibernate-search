/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
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
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchBootstrapIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy elasticsearchClientSpy = new ElasticsearchClientSpy();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

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
				.withIndex( index )
				.setupFirstPhaseOnly();

		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isZero();

		// In the *second* phase, however, we expect the client to be created and used to check the version
		elasticsearchClientSpy.expectNext(
				ElasticsearchRequest.get().build(), ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		partialSetup.doSecondPhase();
		elasticsearchClientSpy.verifyExpectationsMet();

		checkBackendWorks();
	}

	/**
	 * Check that an exception is thrown when version_check.enabled is false
	 * without specifying the Elasticsearch version.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3841")
	public void noVersionCheck_missingVersion() {
		assertThatThrownBy(
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
						.failure( "Invalid value for configuration property 'hibernate.search.backend.version': ''",
								"Missing or imprecise Elasticsearch version",
								"when configuration property 'hibernate.search.backend.version_check.enabled' is set to 'false'",
								"the version is mandatory and must be at least as precise as 'x.y', where 'x' and 'y' are integers" )
						.build()
				);
	}

	/**
	 * Check that an exception is thrown when version_check.enabled is false
	 * while specifying only the major number of the Elasticsearch version.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3841")
	public void noVersionCheck_incompleteVersion() {
		ElasticsearchVersion clusterVersion = ElasticsearchVersion.of( ElasticsearchTestDialect.getClusterVersion() );
		String versionWithMajorOnly = String.valueOf( clusterVersion.major() );

		assertThatThrownBy(
				() -> setupHelper.start()
						.withBackendProperty(
								ElasticsearchBackendSettings.VERSION_CHECK_ENABLED, false
						)
						.withBackendProperty(
								ElasticsearchBackendSettings.VERSION, versionWithMajorOnly
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
						.failure( "Invalid value for configuration property 'hibernate.search.backend.version': '" + versionWithMajorOnly + "'",
								"Missing or imprecise Elasticsearch version",
								"when configuration property 'hibernate.search.backend.version_check.enabled' is set to 'false'",
								"the version is mandatory and must be at least as precise as 'x.y', where 'x' and 'y' are integers" )
						.build()
				);
	}

	/**
	 * Check everything works fine when version_check.enabled is false
	 * and specifying the major and minor number of the Elasticsearch version.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-3841", "HSEARCH-4214"})
	public void noVersionCheck_completeVersion() {
		ElasticsearchVersion clusterVersion = ElasticsearchVersion.of( ElasticsearchTestDialect.getClusterVersion() );
		String versionWithMajorAndMinorOnly = clusterVersion.major() + "." + clusterVersion.minor().getAsInt();

		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION_CHECK_ENABLED, false
				)
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION, versionWithMajorAndMinorOnly
				)
				.withBackendProperty(
						ElasticsearchBackendSpiSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.factoryReference()
				)
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( index )
				.setupFirstPhaseOnly();
		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isZero();

		partialSetup.doSecondPhase();
		// We do not expect any request, since the version check is disabled
		assertThat( elasticsearchClientSpy.getRequestCount() ).isZero();

		checkBackendWorks();

		assertThat( elasticsearchClientSpy.getRequestCount() ).isNotZero();
	}

	private void checkBackendWorks() {
		index.schemaManager().createIfMissing().join();
		assertThatQuery( index.query().where( f -> f.matchAll() ) ).hasNoHits();
		index.index( "1", document -> { } );
		assertThatQuery( index.query().where( f -> f.matchAll() ) ).hasDocRefHitsAnyOrder( index.typeName(), "1" );
	}
}
