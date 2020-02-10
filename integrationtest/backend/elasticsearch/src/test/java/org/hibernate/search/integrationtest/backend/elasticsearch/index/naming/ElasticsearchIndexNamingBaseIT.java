/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.naming;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test the base functionality of type name mapping strategies.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchIndexNamingBaseIT {

	private static final String INDEX1_NAME = "index1_name";
	private static final String INDEX2_NAME = "index2_name";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	@Test
	public void nameConflict_aliasesOfSingleIndex() {
		SubTest.expectException( () -> setup( hardcodedStrategy(
				"alias-conflicting", "alias-conflicting",
				"index2-write", "index2-read"
		) ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.indexContext( INDEX1_NAME )
								.failure(
										"Write alias and read alias must be different,"
												+ " but were set to the same value: 'alias-conflicting'."
								)
								.build()
				);
	}

	@Test
	public void crossIndexNameConflict_writeAliasAndWriteAlias() {
		setupExpectingCrossIndexNameConflict(
				"alias-conflicting", "index1-read", // Index 1 aliases
				"alias-conflicting", "index2-read", // Index 2 aliases
				"alias-conflicting" // This name is used by both indexes
		);
	}

	@Test
	public void crossIndexNameConflict_readAliasAndReadAlias() {
		setupExpectingCrossIndexNameConflict(
				"index1-write", "alias-conflicting", // Index 1 aliases
				"index2-write", "alias-conflicting", // Index 2 aliases
				"alias-conflicting" // This name is used by both indexes
		);
	}

	@Test
	public void crossIndexNameConflict_writeAliasAndReadAlias() {
		setupExpectingCrossIndexNameConflict(
				"alias-conflicting", "index1-write", // Index 1 aliases
				"index2-write", "alias-conflicting", // Index 2 aliases
				"alias-conflicting" // This name is used by both indexes
		);
	}

	@Test
	public void crossIndexNameConflict_hibernateSearchNameAndWriteAlias() {
		setupExpectingCrossIndexNameConflict(
				INDEX2_NAME, "index1-read", // Index 1 aliases
				"index2-write", "index2-read", // Index 2 aliases
				INDEX2_NAME // This name is used by both indexes
		);
	}

	@Test
	public void crossIndexNameConflict_hibernateSearchNameAndReadAlias() {
		setupExpectingCrossIndexNameConflict(
				"index1-write", INDEX2_NAME, // Index 1 aliases
				"index2-write", "index2-read", // Index 2 aliases
				INDEX2_NAME // This name is used by both indexes
		);
	}

	private void setupExpectingCrossIndexNameConflict(String index1WriteAlias, String index1ReadAlias,
			String index2WriteAlias, String index2ReadAlias, String conflictingName) {
		SubTest.expectException( () -> setup( hardcodedStrategy(
				index1WriteAlias, index1ReadAlias,
				index2WriteAlias, index2ReadAlias
		) ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.failure(
										"Conflicting index names: Hibernate Search indexes '" + INDEX1_NAME
												+ "' and '" + INDEX2_NAME + "' both target the name or alias '"
												+ conflictingName + "'"
								)
								.build()
				);
	}

	private void setup(IndexLayoutStrategy strategy) {
		setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.LAYOUT_STRATEGY,
						strategy
				)
				.withIndex( INDEX1_NAME, ctx -> { } )
				.withIndex( INDEX2_NAME, ctx -> { } )
				.setup();
	}

	private static IndexLayoutStrategy hardcodedStrategy(String index1WriteAlias, String index1ReadAlias,
			String index2WriteAlias, String index2ReadAlias) {
		return new IndexLayoutStrategy() {
			@Override
			public String createInitialElasticsearchIndexName(String hibernateSearchIndexName) {
				return hibernateSearchIndexName + "-actual";
			}

			@Override
			public String createWriteAlias(String hibernateSearchIndexName) {
				switch ( hibernateSearchIndexName ) {
					case INDEX1_NAME:
						return index1WriteAlias;
					case INDEX2_NAME:
						return index2WriteAlias;
					default:
						throw unexpectedIndex( hibernateSearchIndexName );
				}
			}

			@Override
			public String createReadAlias(String hibernateSearchIndexName) {
				switch ( hibernateSearchIndexName ) {
					case INDEX1_NAME:
						return index1ReadAlias;
					case INDEX2_NAME:
						return index2ReadAlias;
					default:
						throw unexpectedIndex( hibernateSearchIndexName );
				}
			}

			private IllegalArgumentException unexpectedIndex(String hibernateSearchIndexName) {
				throw new IllegalArgumentException( "Unexpected index: " + hibernateSearchIndexName );
			}
		};
	}

}
