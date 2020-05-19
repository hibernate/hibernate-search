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
import org.assertj.core.api.Assertions;

import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test the base functionality of type name mapping strategies.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchIndexNamingBaseIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public final TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index1 = StubMappedIndex.withoutFields().name( "index1" );
	private final StubMappedIndex index2 = StubMappedIndex.withoutFields().name( "index2" );

	@Test
	public void nameConflict_aliasesOfSingleIndex() {
		Assertions.assertThatThrownBy( () -> setup( hardcodedStrategy(
				"alias-conflicting", "alias-conflicting",
				"index2-write", "index2-read"
		) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.indexContext( index1.name() )
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
				index2.name(), "index1-read", // Index 1 aliases
				"index2-write", "index2-read", // Index 2 aliases
				index2.name() // This name is used by both indexes
		);
	}

	@Test
	public void crossIndexNameConflict_hibernateSearchNameAndReadAlias() {
		setupExpectingCrossIndexNameConflict(
				"index1-write", index2.name(), // Index 1 aliases
				"index2-write", "index2-read", // Index 2 aliases
				index2.name() // This name is used by both indexes
		);
	}

	private void setupExpectingCrossIndexNameConflict(String index1WriteAlias, String index1ReadAlias,
			String index2WriteAlias, String index2ReadAlias, String conflictingName) {
		Assertions.assertThatThrownBy( () -> setup( hardcodedStrategy(
				index1WriteAlias, index1ReadAlias,
				index2WriteAlias, index2ReadAlias
		) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching(
						FailureReportUtils.buildFailureReportPattern()
								.failure(
										"Conflicting index names: Hibernate Search indexes '" + index1.name()
												+ "' and '" + index2.name() + "' both target the name or alias '"
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
				.withIndexes( index1, index2 )
				.setup();
	}

	private IndexLayoutStrategy hardcodedStrategy(String index1WriteAlias, String index1ReadAlias,
			String index2WriteAlias, String index2ReadAlias) {
		return new IndexLayoutStrategy() {
			@Override
			public String createInitialElasticsearchIndexName(String hibernateSearchIndexName) {
				return hibernateSearchIndexName + "-actual";
			}

			@Override
			public String createWriteAlias(String hibernateSearchIndexName) {
				if ( index1.name().equals( hibernateSearchIndexName ) ) {
					return index1WriteAlias;
				}
				else if ( index2.name().equals( hibernateSearchIndexName ) ) {
					return index2WriteAlias;
				}
				else {
					throw unexpectedIndex( hibernateSearchIndexName );
				}
			}

			@Override
			public String createReadAlias(String hibernateSearchIndexName) {
				if ( index1.name().equals( hibernateSearchIndexName ) ) {
					return index1ReadAlias;
				}
				else if ( index2.name().equals( hibernateSearchIndexName ) ) {
					return index2ReadAlias;
				}
				else {
					throw unexpectedIndex( hibernateSearchIndexName );
				}
			}

			private IllegalArgumentException unexpectedIndex(String hibernateSearchIndexName) {
				throw new IllegalArgumentException( "Unexpected index: " + hibernateSearchIndexName );
			}
		};
	}

}
