/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect.isActualVersion;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;
import static org.junit.Assume.assumeFalse;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
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

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

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
						ElasticsearchBackendSettings.VERSION, ElasticsearchTestDialect.getActualVersion().toString()
				)
				.withBackendProperty(
						ElasticsearchBackendImplSettings.CLIENT_FACTORY,
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
								ElasticsearchBackendImplSettings.CLIENT_FACTORY,
								elasticsearchClientSpy.factoryReference()
						)
						.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
						.withIndex( StubMappedIndex.withoutFields() )
						.setupFirstPhaseOnly(),
				"NO version check without explicit version number"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure( "Invalid value for configuration property 'hibernate.search.backend.version': ''",
								"Missing or imprecise Elasticsearch version",
								"when configuration property 'hibernate.search.backend.version_check.enabled' is set to 'false'",
								"the version is mandatory and must be at least as precise as 'x.y', where 'x' and 'y' are integers" )
				);
	}

	/**
	 * Check that an exception is thrown when version_check.enabled is false
	 * while specifying only the major number of the Elasticsearch version.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3841")
	public void noVersionCheck_incompleteVersion() {
		assumeFalse(
				"This test only is only relevant" +
						" for Elasticsearch major versions where all minor versions" +
						" use the same model dialect." +
						" It is not the case on ES 5 in particular, since 5.6 has a dialect" +
						" but 5.0, 5.1, etc. don't have one.",
				isActualVersion(
						esVersion -> esVersion.isAtMost( "5.6" ),
						osVersion -> false
				)
		);
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();
		String versionWithMajorOnly = actualVersion.distribution() + ":" + actualVersion.major();

		assertThatThrownBy(
				() -> setupHelper.start()
						.withBackendProperty(
								ElasticsearchBackendSettings.VERSION_CHECK_ENABLED, false
						)
						.withBackendProperty(
								ElasticsearchBackendSettings.VERSION, versionWithMajorOnly
						)
						.withBackendProperty(
								ElasticsearchBackendImplSettings.CLIENT_FACTORY,
								elasticsearchClientSpy.factoryReference()
						)
						.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
						.withIndex( StubMappedIndex.withoutFields() )
						.setup(),
				"NO version check with partial version number"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure( "Invalid value for configuration property 'hibernate.search.backend.version': '" + versionWithMajorOnly + "'",
								"Missing or imprecise Elasticsearch version",
								"when configuration property 'hibernate.search.backend.version_check.enabled' is set to 'false'",
								"the version is mandatory and must be at least as precise as 'x.y', where 'x' and 'y' are integers" )
				);
	}

	/**
	 * Check everything works fine when version_check.enabled is false
	 * and specifying the major and minor number of the Elasticsearch version.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-3841", "HSEARCH-4214"})
	public void noVersionCheck_completeVersion() {
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();
		String versionWithMajorAndMinorOnly = actualVersion.distribution() + ":"
				+ actualVersion.major() + "." + actualVersion.minor().getAsInt();

		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION, versionWithMajorAndMinorOnly
				)
				.withBackendProperty(
						ElasticsearchBackendImplSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.factoryReference()
				)
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( index )
				.setupFirstPhaseOnly();
		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isZero();

		Map<String, Object> runtimeProperties = new HashMap<>();
		runtimeProperties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION_CHECK_ENABLED ), false );
		partialSetup.doSecondPhase( AllAwareConfigurationPropertySource.fromMap( runtimeProperties ) );
		// We do not expect any request, since the version check is disabled
		assertThat( elasticsearchClientSpy.getRequestCount() ).isZero();

		checkBackendWorks();

		assertThat( elasticsearchClientSpy.getRequestCount() ).isNotZero();
	}

	/**
	 * Check that an exception is thrown when version_check.enabled is false
	 * and specifying a version on backend creation, and a different one on backend start.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4214")
	public void noVersionCheck_versionOverrideOnStart_incompatibleVersion() {
		assumeFalse(
				"This test only is only relevant" +
						" for Elasticsearch major versions where all minor versions" +
						" use the same model dialect." +
						" It is not the case on ES 5 in particular, since 5.6 has a dialect" +
						" but 5.0, 5.1, etc. don't have one.",
				isActualVersion(
						esVersion -> esVersion.isAtMost( "5.6" ),
						osVersion -> false
				)
		);
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();
		String versionWithMajorOnly = actualVersion.distribution() + ":" + actualVersion.major();
		String incompatibleVersionWithMajorAndMinorOnly = actualVersion.distribution() + ":"
				+ ( actualVersion.major() == 2 ? "42." : "2." ) + actualVersion.minor().getAsInt();

		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION, versionWithMajorOnly
				)
				.withBackendProperty(
						ElasticsearchBackendImplSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.factoryReference()
				)
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( index )
				.setupFirstPhaseOnly();
		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isZero();

		Map<String, Object> runtimeProperties = new HashMap<>();
		runtimeProperties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION_CHECK_ENABLED ), false );
		runtimeProperties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION ), incompatibleVersionWithMajorAndMinorOnly );
		assertThatThrownBy(
				() -> partialSetup.doSecondPhase( AllAwareConfigurationPropertySource.fromMap( runtimeProperties ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure(
								"Invalid value for configuration property 'hibernate.search.backend.version': '"
										+ incompatibleVersionWithMajorAndMinorOnly + "'",
								"Incompatible Elasticsearch version:"
										+ " version '" + incompatibleVersionWithMajorAndMinorOnly
										+ "' does not match version '" + versionWithMajorOnly + "' that was provided"
										+ " when the backend was created.",
								"You can provide a more precise version on startup,"
										+ " but you cannot override the version that was provided when the backend was created." )
				);
	}

	/**
	 * Check everything works fine when version_check.enabled is false
	 * and specifying a version on backend creation, and a more precise one on backend start.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4214")
	public void noVersionCheck_versionOverrideOnStart_compatibleVersion() {
		assumeFalse(
				"This test only is only relevant" +
						" for Elasticsearch major versions where all minor versions" +
						" use the same model dialect." +
						" It is not the case on ES 5 in particular, since 5.6 has a dialect" +
						" but 5.0, 5.1, etc. don't have one.",
				isActualVersion(
						esVersion -> esVersion.isAtMost( "5.6" ),
						osVersion -> false
				)
		);
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();
		String versionWithMajorOnly = actualVersion.distribution() + ":" + actualVersion.major();
		String versionWithMajorAndMinorOnly = actualVersion.distribution() + ":"
				+ actualVersion.major() + "." + actualVersion.minor().getAsInt();

		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION, versionWithMajorOnly
				)
				.withBackendProperty(
						ElasticsearchBackendImplSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.factoryReference()
				)
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( index )
				.setupFirstPhaseOnly();
		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isZero();

		Map<String, Object> runtimeProperties = new HashMap<>();
		runtimeProperties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION_CHECK_ENABLED ), false );
		runtimeProperties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION ), versionWithMajorAndMinorOnly );
		partialSetup.doSecondPhase( AllAwareConfigurationPropertySource.fromMap( runtimeProperties ) );
		// We do not expect any request, since the version check is disabled
		assertThat( elasticsearchClientSpy.getRequestCount() ).isZero();

		checkBackendWorks();

		assertThat( elasticsearchClientSpy.getRequestCount() ).isNotZero();
	}

	/**
	 * Check custom settings/mapping can be parsed when version_check.enabled is false,
	 * meaning the elasticsearch link is only established *after*
	 * the custom settings/mapping are parsed.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-4435"})
	public void noVersionCheck_customSettingsAndMapping() {
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();
		String versionWithMajorAndMinorOnly = actualVersion.distribution() + ":"
				+ actualVersion.major() + "." + actualVersion.minor().getAsInt();

		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty( ElasticsearchBackendSettings.VERSION, versionWithMajorAndMinorOnly )
				.withBackendProperty( ElasticsearchBackendImplSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.factoryReference() )
				.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						"bootstrap-it/custom-settings.json" )
				.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"bootstrap-it/custom-mapping.json" )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( index )
				.setupFirstPhaseOnly();
		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isZero();

		Map<String, Object> runtimeProperties = new HashMap<>();
		runtimeProperties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION_CHECK_ENABLED ), false );
		partialSetup.doSecondPhase( AllAwareConfigurationPropertySource.fromMap( runtimeProperties ) );
		checkBackendWorks();

		assertThat( elasticsearchClient.index( index.name() ).settings( "index.number_of_replicas" ).get() )
				.isEqualTo( "\"42\"" );
		assertJsonEqualsIgnoringUnknownFields(
				"{"
					+ "'properties': {"
							+ "'custom': {"
							+ "}"
					+ "}"
				+ "}",
				elasticsearchClient.index( index.name() ).type().getMapping() );
	}

	private void checkBackendWorks() {
		index.schemaManager().createIfMissing( OperationSubmitter.blocking() ).join();
		assertThatQuery( index.query().where( f -> f.matchAll() ) ).hasNoHits();
		index.index( "1", document -> { } );
		assertThatQuery( index.query().where( f -> f.matchAll() ) ).hasDocRefHitsAnyOrder( index.typeName(), "1" );
	}
}
