/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.work;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.readAliasDefinition;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.writeAliasDefinition;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Demonstrates the feasibility of zero-downtime reindexing,
 * i.e. reindexing without emptying the index, so that searches still return results while reindexing.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
class ElasticsearchZeroDowntimeReindexingIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	void test() {
		IndexIndexer indexer = index.createIndexer();

		indexer.add(
				referenceProvider( "1" ),
				document -> document.addValue( index.binding().text, "text1" ),
				DocumentCommitStrategy.NONE,
				DocumentRefreshStrategy.FORCE,
				OperationSubmitter.blocking()
		).join();

		SearchQuery<DocumentReference> text1Query = index
				.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();
		SearchQuery<DocumentReference> text2Query = index
				.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text2" ) )
				.toQuery();

		// Initial state: text == "text1"
		// In a real-world scenario, we would index thousands of documents
		assertThatQuery( text1Query ).hasTotalHitCount( 1 );
		assertThatQuery( text2Query ).hasNoHits();

		// Create a new index without aliases
		URLEncodedString newIndexPrimaryName = encodeName( index.name() + "-000002" );
		elasticsearchClient.index( newIndexPrimaryName, null, null ).deleteAndCreate();

		// Switch the write alias from the old to the new index
		elasticsearchClient.index( index.name() ).aliases()
				.move( defaultWriteAlias( index.name() ).original, newIndexPrimaryName.original, writeAliasDefinition() );

		// Search queries are unaffected: text == "text1"
		assertThatQuery( text1Query ).hasTotalHitCount( 1 );
		assertThatQuery( text2Query ).hasNoHits();

		// Reindex the document: text == "text2"
		// In a real-world scenario, we would reindex thousands of documents
		indexer.add(
				referenceProvider( "1" ),
				document -> document.addValue( index.binding().text, "text2" ),
				DocumentCommitStrategy.NONE,
				DocumentRefreshStrategy.FORCE,
				OperationSubmitter.blocking()
		).join();

		// Search queries are unaffected: text == "text1"
		assertThatQuery( text1Query ).hasTotalHitCount( 1 );
		assertThatQuery( text2Query ).hasNoHits();

		// Switch the read alias from the old to the new index
		elasticsearchClient.index( index.name() ).aliases()
				.move( defaultReadAlias( index.name() ).original, newIndexPrimaryName.original, readAliasDefinition() );

		// Search queries immediately show the new content: text == "text2"
		assertThatQuery( text1Query ).hasNoHits();
		assertThatQuery( text2Query ).hasTotalHitCount( 1 );

		// Remove the old index
		elasticsearchClient.index( index.name() ).ensureDoesNotExist();

		// Search queries still work and target the new index: text == "text2"
		assertThatQuery( text1Query ).hasNoHits();
		assertThatQuery( text2Query ).hasTotalHitCount( 1 );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> text;

		IndexBinding(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString() )
					.toReference();
		}
	}


}
