/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Demonstrates the feasibility of zero-downtime reindexing,
 * i.e. reindexing without emptying the index, so that searches still return results while reindexing.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
public class ElasticsearchZeroDowntimeReindexingIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void test() {
		IndexWorkspace workspace = index.createWorkspace();
		IndexIndexer indexer = index.createIndexer();

		indexer.add(
				referenceProvider( "1" ),
				document -> document.addValue( index.binding().text, "text1" ),
				DocumentCommitStrategy.NONE,
				DocumentRefreshStrategy.NONE,
				OperationSubmitter.blocking()
		).join();
		workspace.refresh( OperationSubmitter.blocking() ).join();

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
				DocumentRefreshStrategy.NONE,
				OperationSubmitter.blocking()
		).join();
		workspace.refresh( OperationSubmitter.blocking() ).join();

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
