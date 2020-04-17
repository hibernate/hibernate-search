/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class LuceneSimilarityIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final StubMappedIndex index = StubMappedIndex.withoutFields( "MainIndex" );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3777")
	public void defaults() {
		setup( null );

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::getIndexAccessorForTests )
				.extracting( IndexAccessorImpl::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.allSatisfy( config -> {
					assertThat( config.getSimilarity() ).as( "getSimilarity" )
							.isInstanceOf( BM25Similarity.class );
				} );

		// Add a document to the index
		IndexIndexingPlan<?> plan = index.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> { } );
		plan.execute().join();

		// Check that writing succeeded
		SearchResultAssert.assertThat( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasTotalHitCount( 1L );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3777")
	@PortedFromSearch5(original = "org.hibernate.search.test.similarity.SimilarityTest")
	public void custom() {
		setup( new ClassicSimilarity() );

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::getIndexAccessorForTests )
				.extracting( IndexAccessorImpl::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.allSatisfy( config -> {
					assertThat( config.getSimilarity() ).as( "getSimilarity" )
							.isInstanceOf( ClassicSimilarity.class );
				} );

		// Add a document to the index
		IndexIndexingPlan<?> plan = index.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> { } );
		plan.execute().join();

		// Check that writing succeeded
		SearchResultAssert.assertThat( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasTotalHitCount( 1L );
	}

	private SearchIntegration setup(Similarity similarity) {
		return setupHelper.start()
				.withIndex( index )
				.withBackendProperty(
						LuceneBackendSettings.ANALYSIS_CONFIGURER,
						similarity == null ? null
								: (LuceneAnalysisConfigurer) context -> context.similarity( similarity )
				)
				.setup();
	}
}
