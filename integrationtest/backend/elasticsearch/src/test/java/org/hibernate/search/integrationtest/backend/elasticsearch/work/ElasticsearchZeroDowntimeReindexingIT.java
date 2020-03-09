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
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
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

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "indexname";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start( BACKEND_NAME )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void test() {
		IndexWorkspace workspace = indexManager.createWorkspace();
		IndexIndexer indexer = indexManager.createIndexer( DocumentCommitStrategy.NONE );

		indexer.add( referenceProvider( "1" ), document -> {
			document.addValue( indexMapping.text, "text1" );
		} ).join();
		workspace.refresh().join();

		SearchQuery<DocumentReference> text1Query = indexManager
				.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();
		SearchQuery<DocumentReference> text2Query = indexManager
				.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text2" ) )
				.toQuery();

		// Initial state: text == "text1"
		// In a real-world scenario, we would index thousands of documents
		assertThat( text1Query ).hasTotalHitCount( 1 );
		assertThat( text2Query ).hasNoHits();

		// Create a new index without aliases
		URLEncodedString newIndexPrimaryName = encodeName( INDEX_NAME + "-000002" );
		elasticsearchClient.index( newIndexPrimaryName, null, null ).deleteAndCreate();

		// Switch the write alias from the old to the new index
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.move( defaultWriteAlias( INDEX_NAME ).original, newIndexPrimaryName.original, writeAliasDefinition() );

		// Search queries are unaffected: text == "text1"
		assertThat( text1Query ).hasTotalHitCount( 1 );
		assertThat( text2Query ).hasNoHits();

		// Reindex the document: text == "text2"
		// In a real-world scenario, we would reindex thousands of documents
		indexer.add( referenceProvider( "1" ), document -> {
			document.addValue( indexMapping.text, "text2" );
		} ).join();
		workspace.refresh().join();

		// Search queries are unaffected: text == "text1"
		assertThat( text1Query ).hasTotalHitCount( 1 );
		assertThat( text2Query ).hasNoHits();

		// Switch the read alias from the old to the new index
		elasticsearchClient.index( INDEX_NAME ).aliases()
				.move( defaultReadAlias( INDEX_NAME ).original, newIndexPrimaryName.original, readAliasDefinition() );

		// Search queries immediately show the new content: text == "text2"
		assertThat( text1Query ).hasNoHits();
		assertThat( text2Query ).hasTotalHitCount( 1 );

		// Remove the old index
		elasticsearchClient.index( INDEX_NAME ).ensureDoesNotExist();

		// Search queries still work and target the new index: text == "text2"
		assertThat( text1Query ).hasNoHits();
		assertThat( text2Query ).hasTotalHitCount( 1 );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> text;

		IndexMapping(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString() )
					.toReference();
		}
	}


}
