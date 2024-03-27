/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;

class LuceneSimilarityIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3777")
	void defaults() {
		setup( null );

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.isNotEmpty()
				.allSatisfy( config -> {
					assertThat( config.getSimilarity() ).as( "getSimilarity" )
							.isInstanceOf( BM25Similarity.class );
				} );

		// Add a document to the index
		index.index( "1", document -> {} );

		// Check that writing succeeded
		assertThatQuery( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasTotalHitCount( 1L );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3777")
	@PortedFromSearch5(original = "org.hibernate.search.test.similarity.SimilarityTest")
	void custom() {
		setup( new ClassicSimilarity() );

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.isNotEmpty()
				.allSatisfy( config -> {
					assertThat( config.getSimilarity() ).as( "getSimilarity" )
							.isInstanceOf( ClassicSimilarity.class );
				} );

		// Add a document to the index
		index.index( "1", document -> {} );

		// Check that writing succeeded
		assertThatQuery( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasTotalHitCount( 1L );
	}

	private void setup(Similarity similarity) {
		setupHelper.start()
				.withIndex( index )
				.withBackendProperty(
						LuceneBackendSettings.ANALYSIS_CONFIGURER,
						similarity == null
								? null
								: (LuceneAnalysisConfigurer) context -> context.similarity( similarity )
				)
				.setup();
	}
}
