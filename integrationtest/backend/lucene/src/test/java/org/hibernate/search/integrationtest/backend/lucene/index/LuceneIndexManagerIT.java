/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class LuceneIndexManagerIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static LuceneIndexManager indexApi;

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
		indexApi = index.toApi().unwrap( LuceneIndexManager.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexingAnalyzer() throws IOException {
		Analyzer analyzer = indexApi.indexingAnalyzer();
		assertThat( analyze( analyzer, "whitespace_lowercase", "Foo Bar" ) )
				.containsExactly( "foo", "bar" );
		// Overridden with a search analyzer, which should be ignored here
		assertThat( analyze( analyzer, "ngram", "Foo Bar" ) )
				.containsExactly( "foo", "bar" );
		// Normalizer
		assertThat( analyze( analyzer, "normalized", "Foo Bar" ) )
				.containsExactly( "foo bar" );
		// Default for unknown fields: keyword analyzer
		assertThat( analyze( analyzer, "unknown", "Foo Bar" ) )
				.containsExactly( "Foo Bar" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void searchAnalyzer() throws IOException {
		Analyzer analyzer = indexApi.searchAnalyzer();
		assertThat( analyze( analyzer, "whitespace_lowercase", "Foo Bar" ) )
				.containsExactly( "foo", "bar" );
		// Overridden with a search analyzer
		assertThat( analyze( analyzer, "ngram", "Foo Bar" ) )
				.containsExactly( "Foo B", "Foo Ba", "oo Ba", "oo Bar", "o Bar" );
		// Normalizer
		assertThat( analyze( analyzer, "normalized", "Foo Bar" ) )
				.containsExactly( "foo bar" );
		// Default for unknown fields: keyword analyzer
		assertThat( analyze( analyzer, "unknown", "Foo Bar" ) )
				.containsExactly( "Foo Bar" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void computeSizeInBytes() {
		long initialSize = indexApi.computeSizeInBytes();
		assertThat( initialSize ).isGreaterThanOrEqualTo( 0L );

		IndexBinding binding = index.binding();
		index.bulkIndexer()
				.add( 100, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( binding.normalized, "value" + i )
				) )
				.join();

		index.createWorkspace().flush().join();

		long finalSize = indexApi.computeSizeInBytes();
		assertThat( finalSize ).isGreaterThan( 0L );
		assertThat( finalSize ).isGreaterThan( initialSize );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void computeSizeInBytesAsync() {
		CompletableFuture<Long> initialSizeFuture = indexApi.computeSizeInBytesAsync().toCompletableFuture();
		await().untilAsserted( () -> assertThat( initialSizeFuture ).isCompleted() );
		long initialSize = initialSizeFuture.join();
		assertThat( initialSize ).isGreaterThanOrEqualTo( 0L );

		IndexBinding binding = index.binding();
		index.bulkIndexer()
				.add( 100, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( binding.normalized, "value" + i )
				) )
				.join();

		index.createWorkspace().flush().join();

		CompletableFuture<Long> finalSizeFuture = indexApi.computeSizeInBytesAsync().toCompletableFuture();
		await().untilAsserted( () -> assertThat( finalSizeFuture ).isCompleted() );
		long finalSize = finalSizeFuture.join();
		assertThat( finalSize ).isGreaterThan( 0L );
		assertThat( finalSize ).isGreaterThan( initialSize );
	}

	private List<String> analyze(Analyzer analyzer, String absoluteFieldPath, String inputString) throws IOException {
		final List<String> tokenList = new ArrayList<>();
		try ( TokenStream stream = analyzer.tokenStream( absoluteFieldPath, inputString ) ) {
			CharTermAttribute term = stream.addAttribute( CharTermAttribute.class );
			stream.reset();
			while ( stream.incrementToken() ) {
				String s = new String( term.buffer(), 0, term.length() );
				tokenList.add( s );
			}
			stream.end();
		}
		return tokenList;
	}

	private static class IndexBinding {
		private final IndexFieldReference<String> normalized;

		public IndexBinding(IndexSchemaElement root) {
			root.field( "whitespace_lowercase", f -> f.asString()
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
					.toReference();
			root.field( "ngram", f -> f.asString()
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
					.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name ) )
					.toReference();
			normalized = root.field( "normalized", f -> f.asString()
					.normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) )
					.toReference();
		}
	}
}
