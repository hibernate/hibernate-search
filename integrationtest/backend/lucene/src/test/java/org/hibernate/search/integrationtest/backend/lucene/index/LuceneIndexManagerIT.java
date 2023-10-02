/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneAnalysisUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.analysis.Analyzer;

class LuceneIndexManagerIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static LuceneIndexManager indexApi;

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
		indexApi = index.toApi().unwrap( LuceneIndexManager.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void indexingAnalyzer() throws IOException {
		Analyzer analyzer = indexApi.indexingAnalyzer();
		assertThat( LuceneAnalysisUtils.analyze( analyzer, "whitespace_lowercase", "Foo Bar" ) )
				.containsExactly( "foo", "bar" );
		// Overridden with a search analyzer, which should be ignored here
		assertThat( LuceneAnalysisUtils.analyze( analyzer, "ngram", "Foo Bar" ) )
				.containsExactly( "foo", "bar" );
		// Normalizer
		assertThat( LuceneAnalysisUtils.analyze( analyzer, "normalized", "Foo Bar" ) )
				.containsExactly( "foo bar" );
		// Default for unknown fields: keyword analyzer
		assertThat( LuceneAnalysisUtils.analyze( analyzer, "unknown", "Foo Bar" ) )
				.containsExactly( "Foo Bar" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void searchAnalyzer() throws IOException {
		Analyzer analyzer = indexApi.searchAnalyzer();
		assertThat( LuceneAnalysisUtils.analyze( analyzer, "whitespace_lowercase", "Foo Bar" ) )
				.containsExactly( "foo", "bar" );
		// Overridden with a search analyzer
		assertThat( LuceneAnalysisUtils.analyze( analyzer, "ngram", "Foo Bar" ) )
				.containsExactly( "Foo B", "Foo Ba", "oo Ba", "oo Bar", "o Bar" );
		// Normalizer
		assertThat( LuceneAnalysisUtils.analyze( analyzer, "normalized", "Foo Bar" ) )
				.containsExactly( "foo bar" );
		// Default for unknown fields: keyword analyzer
		assertThat( LuceneAnalysisUtils.analyze( analyzer, "unknown", "Foo Bar" ) )
				.containsExactly( "Foo Bar" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void computeSizeInBytes() {
		long initialSize = indexApi.computeSizeInBytes();
		assertThat( initialSize ).isGreaterThanOrEqualTo( 0L );

		IndexBinding binding = index.binding();
		index.bulkIndexer()
				.add( 100, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( binding.normalized, "value" + i )
				) )
				.join();

		index.createWorkspace().flush( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();

		long finalSize = indexApi.computeSizeInBytes();
		assertThat( finalSize ).isGreaterThan( 0L );
		assertThat( finalSize ).isGreaterThan( initialSize );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void computeSizeInBytesAsync() {
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

		index.createWorkspace().flush( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();

		CompletableFuture<Long> finalSizeFuture = indexApi.computeSizeInBytesAsync().toCompletableFuture();
		await().untilAsserted( () -> assertThat( finalSizeFuture ).isCompleted() );
		long finalSize = finalSizeFuture.join();
		assertThat( finalSize ).isGreaterThan( 0L );
		assertThat( finalSize ).isGreaterThan( initialSize );
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
