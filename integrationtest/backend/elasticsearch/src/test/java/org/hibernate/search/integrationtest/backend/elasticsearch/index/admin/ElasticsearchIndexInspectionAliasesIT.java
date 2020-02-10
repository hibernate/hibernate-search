/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.admin;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests related to aliases when inspecting existing indexes,
 * for all applicable index lifecycle strategies.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchIndexInspectionAliasesIT {

	private static final String INDEX_NAME = "IndexName";

	@Parameters(name = "With strategy {0}")
	public static EnumSet<IndexLifecycleStrategyName> strategies() {
		return EnumSet.complementOf( EnumSet.of(
				// This strategies doesn't inspect the existing indexes, so we don't test it
				IndexLifecycleStrategyName.NONE
		) );
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final IndexLifecycleStrategyName strategy;

	public ElasticsearchIndexInspectionAliasesIT(IndexLifecycleStrategyName strategy) {
		this.strategy = strategy;
	}

	@Test
	public void writeAliasTargetsMultipleIndexes() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate();
		// The write alias for index 1 also targets a second index
		elasticsearchClient.index( "otherIndex" )
				.deleteAndCreate()
				.aliases().put( defaultWriteAlias( INDEX_NAME ).original );

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
								.failure(
										"Index aliases [" + defaultWriteAlias( INDEX_NAME ) + ", " + defaultReadAlias( INDEX_NAME )
												+ "] are assigned to a single Hibernate Search index, "
												+ " but they are already defined in Elasticsearch and point to multiple distinct indexes: "
												+ "[" + defaultPrimaryName( INDEX_NAME ) + ", "
												+ defaultPrimaryName( "otherIndex" ) + "]"
								)
								.build()
				);
	}

	@Test
	public void readAliasTargetsMultipleIndexes() {
		elasticsearchClient.index( INDEX_NAME )
				.deleteAndCreate();
		// The read alias for index 1 also targets a second index
		elasticsearchClient.index( "otherIndex" )
				.deleteAndCreate()
				.aliases().put( defaultReadAlias( INDEX_NAME ).original );

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
								.failure(
										"Index aliases [" + defaultWriteAlias( INDEX_NAME ) + ", " + defaultReadAlias( INDEX_NAME )
												+ "] are assigned to a single Hibernate Search index, "
												+ " but they are already defined in Elasticsearch and point to multiple distinct indexes: "
												+ "[" + defaultPrimaryName( INDEX_NAME ) + ", "
												+ defaultPrimaryName( "otherIndex" ) + "]"
								)
								.build()
				);
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
