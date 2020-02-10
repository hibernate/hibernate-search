/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.admin;

import static org.hibernate.search.integrationtest.backend.elasticsearch.index.admin.ElasticsearchAdminTestUtils.simpleAliasDefinition;
import static org.hibernate.search.integrationtest.backend.elasticsearch.index.admin.ElasticsearchAdminTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories.RequiresIndexAliasIsWriteIndex;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests related to aliases when validating indexes.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchIndexValidationAliasesIT {

	private static final String SCHEMA_VALIDATION_CONTEXT = "schema validation";

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	@Test
	public void success_simple() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );

		startSetupWithLifecycleStrategy()
				.withIndex( INDEX_NAME, ctx -> { } )
				.setup();

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void writeAlias_missing() {
		elasticsearchClient.index( defaultPrimaryName( INDEX_NAME ), null, defaultReadAlias( INDEX_NAME ) )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );

		SubTest.expectException(
				() -> startSetupWithLifecycleStrategy()
						.withIndex( INDEX_NAME, ctx -> { } )
						.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.indexContext( INDEX_NAME )
								.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
								.aliasContext( defaultWriteAlias( INDEX_NAME ).original )
								.failure( "Missing alias" )
								.build()
				);
	}

	@Test
	public void writeAlias_invalid_filter() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put(
						defaultWriteAlias( INDEX_NAME ).original,
						simpleAliasDefinition( true, "'filter': {'term': {'user_id': 12}}" )
				);

		SubTest.expectException(
				() -> startSetupWithLifecycleStrategy()
						.withIndex( INDEX_NAME, ctx -> { } )
						.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.indexContext( INDEX_NAME )
								.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
								.aliasContext( defaultWriteAlias( INDEX_NAME ).original )
								.aliasAttributeContext( "filter" )
								.failure( "Invalid value. Expected 'null', actual is '{\"term\":{\"user_id\":12}}'" )
								.build()
				);
	}

	@Test
	@Category(RequiresIndexAliasIsWriteIndex.class)
	public void writeAlias_invalid_isWriteIndex() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( defaultWriteAlias( INDEX_NAME ).original, simpleAliasDefinition( false, "" ) );

		SubTest.expectException(
				() -> startSetupWithLifecycleStrategy()
						.withIndex( INDEX_NAME, ctx -> { } )
						.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.indexContext( INDEX_NAME )
								.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
								.aliasContext( defaultWriteAlias( INDEX_NAME ).original )
								.aliasAttributeContext( "is_write_index" )
								.failure( "Invalid value. Expected 'true', actual is 'false'" )
								.build()
				);
	}

	@Test
	public void readAlias_missing() {
		elasticsearchClient.index( defaultPrimaryName( INDEX_NAME ), defaultWriteAlias( INDEX_NAME ), null )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );

		SubTest.expectException(
				() -> startSetupWithLifecycleStrategy()
						.withIndex( INDEX_NAME, ctx -> { } )
						.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.indexContext( INDEX_NAME )
								.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
								.aliasContext( defaultReadAlias( INDEX_NAME ).original )
								.failure( "Missing alias" )
								.build()
				);
	}

	@Test
	public void readAlias_invalid_filter() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.put( defaultReadAlias( INDEX_NAME ).original, "{'filter': {'term': {'user_id': 12}}}" );

		SubTest.expectException(
				() -> startSetupWithLifecycleStrategy()
						.withIndex( INDEX_NAME, ctx -> { } )
						.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.indexContext( INDEX_NAME )
								.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
								.aliasContext( defaultReadAlias( INDEX_NAME ).original )
								.aliasAttributeContext( "filter" )
								.failure( "Invalid value. Expected 'null', actual is '{\"term\":{\"user_id\":12}}'" )
								.build()
				);
	}

	private SearchSetupHelper.SetupContext startSetupWithLifecycleStrategy() {
		return setupHelper.start()
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.VALIDATE.getExternalRepresentation()
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
