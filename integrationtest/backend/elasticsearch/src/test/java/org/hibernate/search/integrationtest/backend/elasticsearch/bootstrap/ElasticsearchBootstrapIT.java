/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
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
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchBootstrapIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public ElasticsearchClientSpy elasticsearchClientSpy = ElasticsearchClientSpy.create();

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	/**
	 * Check that we boot successfully when the Elasticsearch model dialect is determined
	 * from an explicitly configured Elasticsearch version,
	 * and that the Elasticsearch client starts in the second phase of bootstrap in that case.
	 */
	@Test
	void explicitModelDialect() {
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

		// In the *second* phase, however, we expect the client to be created
		// and if possible used to check the version
		if ( ElasticsearchTckBackendFeatures.supportsVersionCheck() ) {
			elasticsearchClientSpy.expectNext(
					ElasticsearchRequest.get().build(), ElasticsearchRequestAssertionMode.EXTENSIBLE
			);
		}
		partialSetup.doSecondPhase();
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isNotZero();
		elasticsearchClientSpy.verifyExpectationsMet();

		checkBackendWorks();
	}

	/**
	 * Check that an exception is thrown when version_check.enabled is false
	 * without specifying the Elasticsearch version.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3841")
	void noVersionCheck_missingVersion() {
		assumeTrue(
				ElasticsearchTckBackendFeatures.supportsVersionCheck(),
				"This test only makes sense if version checks are supported"
		);

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
								"configuration property 'hibernate.search.backend.version_check.enabled' is set to 'false'",
								"you must set the version explicitly with at least as much precision as 'x.y', where 'x' and 'y' are integers" )
				);
	}

	/**
	 * Check that an exception is thrown when version checks are not used
	 * while specifying only the major number of the Elasticsearch version.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3841")
	void noVersionCheck_incompleteVersion() {
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();

		assumeTrue(
				actualVersion.majorOptional().isPresent() && actualVersion.minor().isPresent(),
				"This test only makes sense if the Elasticsearch version has both a major and minor number"
		);
		String versionWithMajorOnly = actualVersion.distribution() + ":" + actualVersion.majorOptional().getAsInt();

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
						.failure(
								"Invalid value for configuration property 'hibernate.search.backend.version': '"
										+ versionWithMajorOnly + "'",
								"Missing or imprecise Elasticsearch version",
								"configuration property 'hibernate.search.backend.version_check.enabled' is set to 'false'",
								"you must set the version explicitly with at least as much precision as 'x.y', where 'x' and 'y' are integers" )
				);
	}

	/**
	 * Check that an exception is thrown when version checks are not used
	 * and specifying a precise enough Elasticsearch version
	 * (generally major and minor number, but may be less e.g. on Amazon OpenSearch Serverless).
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3841", "HSEARCH-4214" })
	void noVersionCheck_completeVersion() {
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();

		String configuredVersion;
		if ( actualVersion.majorOptional().isPresent() && actualVersion.minor().isPresent() ) {
			configuredVersion = actualVersion.distribution() + ":"
					+ actualVersion.majorOptional().getAsInt() + "." + actualVersion.minor().getAsInt();
		}
		else {
			// This should only happen when testing Amazon OpenSearch Serverless
			checkAmazonOpenSearchServerless();
			configuredVersion = "amazon-opensearch-serverless";
		}

		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION, configuredVersion
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
		ensureNoVersionCheck( runtimeProperties );
		partialSetup.doSecondPhase( AllAwareConfigurationPropertySource.fromMap( runtimeProperties ) );
		// We do not expect any request, since the version check is disabled
		assertThat( elasticsearchClientSpy.getRequestCount() ).isZero();

		checkBackendWorks();

		assertThat( elasticsearchClientSpy.getRequestCount() ).isNotZero();
	}

	/**
	 * Check that an exception is thrown when version checks are not used
	 * and specifying a version on backend creation, and a different one on backend start.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4214")
	void noVersionCheck_versionOverrideOnStart_incompatibleVersion() {
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();

		String configuredVersionOnBackendCreation;
		String incompatibleConfiguredVersionOnBackendStart;
		if ( actualVersion.majorOptional().isPresent() && actualVersion.minor().isPresent() ) {
			// This is the case we're most interested in, with major version mismatches
			configuredVersionOnBackendCreation = actualVersion.distribution() + ":"
					+ actualVersion.majorOptional().getAsInt();
			incompatibleConfiguredVersionOnBackendStart = actualVersion.distribution() + ":"
					+ ( actualVersion.majorOptional().getAsInt() == 2 ? "42." : "2." ) + actualVersion.minor()
							.getAsInt();
		}
		else {
			// This should only happen when testing Amazon OpenSearch Serverless
			checkAmazonOpenSearchServerless();
			configuredVersionOnBackendCreation = actualVersion.toString();
			incompatibleConfiguredVersionOnBackendStart = "opensearch:99.9";
		}

		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.VERSION, configuredVersionOnBackendCreation
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
		ensureNoVersionCheck( runtimeProperties );
		runtimeProperties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION ),
				incompatibleConfiguredVersionOnBackendStart );
		assertThatThrownBy(
				() -> partialSetup.doSecondPhase( AllAwareConfigurationPropertySource.fromMap( runtimeProperties ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure(
								"Invalid value for configuration property 'hibernate.search.backend.version': '"
										+ incompatibleConfiguredVersionOnBackendStart + "'",
								"Incompatible Elasticsearch version:"
										+ " version '" + incompatibleConfiguredVersionOnBackendStart
										+ "' does not match version '" + configuredVersionOnBackendCreation
										+ "' that was provided"
										+ " when the backend was created.",
								"You can provide a more precise version on startup,"
										+ " but you cannot override the version that was provided when the backend was created." )
				);
	}

	/**
	 * Check everything works fine when version checks are not used
	 * and specifying a version on backend creation, and a more precise one on backend start.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4214")
	void noVersionCheck_versionOverrideOnStart_compatibleVersion() {
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();

		assumeTrue(
				actualVersion.majorOptional().isPresent() && actualVersion.minor().isPresent(),
				"This test only makes sense if the Elasticsearch version has both a major and minor number"
		);
		String versionWithMajorOnly = actualVersion.distribution() + ":" + actualVersion.majorOptional().getAsInt();
		String versionWithMajorAndMinorOnly = actualVersion.distribution() + ":"
				+ actualVersion.majorOptional().getAsInt() + "." + actualVersion.minor().getAsInt();

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
		runtimeProperties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION ),
				versionWithMajorAndMinorOnly );
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
	@TestForIssue(jiraKey = { "HSEARCH-4435" })
	void noVersionCheck_customSettingsAndMapping() {
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();

		String configuredVersion;
		if ( actualVersion.majorOptional().isPresent() && actualVersion.minor().isPresent() ) {
			configuredVersion = actualVersion.distribution() + ":"
					+ actualVersion.majorOptional().getAsInt() + "." + actualVersion.minor().getAsInt();
		}
		else {
			// This should only happen when testing Amazon OpenSearch Serverless
			checkAmazonOpenSearchServerless();
			configuredVersion = actualVersion.toString();
		}

		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start()
				.withBackendProperty( ElasticsearchBackendSettings.VERSION, configuredVersion )
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
		ensureNoVersionCheck( runtimeProperties );
		partialSetup.doSecondPhase( AllAwareConfigurationPropertySource.fromMap( runtimeProperties ) );
		checkBackendWorks();

		assertThat( elasticsearchClient.index( index.name() ).settings( "index.number_of_replicas" ).get() )
				.isEqualTo( "\"42\"" );
		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'properties': {"
						+ "   'custom': {"
						+ "   }"
						+ " }"
						+ "}",
				elasticsearchClient.index( index.name() ).type().getMapping() );
	}

	private static void ensureNoVersionCheck(Map<String, Object> properties) {
		if ( ElasticsearchTckBackendFeatures.supportsVersionCheck() ) {
			properties.put( BackendSettings.backendKey( ElasticsearchBackendSettings.VERSION_CHECK_ENABLED ),
					false );
		}
		// Else nothing to do: version checks are unsupported and disabled by default.
	}

	private void checkAmazonOpenSearchServerless() {
		ElasticsearchVersion actualVersion = ElasticsearchTestDialect.getActualVersion();
		if ( actualVersion.distribution() != ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS ) {
			throw new IllegalStateException(
					"Unexpected actual Elasticsearch version: " + actualVersion + ". Tests are buggy?" );
		}
	}

	private void checkBackendWorks() {
		index.schemaManager().createIfMissing( OperationSubmitter.blocking() ).join();
		assertThatQuery( index.query().where( f -> f.matchAll() ) ).hasNoHits();
		index.index( "1", document -> {} );
		assertThatQuery( index.query().where( f -> f.matchAll() ) ).hasDocRefHitsAnyOrder( index.typeName(), "1" );
	}
}
