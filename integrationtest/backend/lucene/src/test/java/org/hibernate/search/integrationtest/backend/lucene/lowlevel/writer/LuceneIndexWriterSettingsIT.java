/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.data.Percentage.withPercentage;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.LoggerInfoStream;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.util.InfoStream;

public class LuceneIndexWriterSettingsIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3776")
	public void defaults() {
		setup( properties -> {} );

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.isNotEmpty()
				.allSatisfy( config -> {
					assertSoftly( softly -> {
						softly.assertThat( config.getMaxBufferedDocs() ).as( "getMaxBufferedDocs" )
								.isEqualTo( IndexWriterConfig.DEFAULT_MAX_BUFFERED_DOCS );
						softly.assertThat( config.getRAMBufferSizeMB() ).as( "getRAMBufferSizeMB" )
								.isEqualTo( IndexWriterConfig.DEFAULT_RAM_BUFFER_SIZE_MB );
						softly.assertThat( config.getInfoStream() ).as( "getInfoStream" )
								.isSameAs( InfoStream.NO_OUTPUT );
					} );

					MergePolicy abstractMergePolicy = config.getMergePolicy();
					assertThat( abstractMergePolicy ).as( "getMergePolicy" ).isNotNull();
					LogByteSizeMergePolicy mergePolicy = (LogByteSizeMergePolicy) abstractMergePolicy;

					assertSoftly( softly -> {
						softly.assertThat( mergePolicy.getMaxMergeDocs() ).as( "getMaxMergeDocs()" )
								.isEqualTo( LogByteSizeMergePolicy.DEFAULT_MAX_MERGE_DOCS );
						softly.assertThat( mergePolicy.getMergeFactor() ).as( "getMergeFactor()" )
								.isEqualTo( LogByteSizeMergePolicy.DEFAULT_MERGE_FACTOR );
						softly.assertThat( mergePolicy.getMinMergeMB() ).as( "getMinMergeMB()" )
								.isCloseTo( LogByteSizeMergePolicy.DEFAULT_MIN_MERGE_MB, withPercentage( 1 ) );
						softly.assertThat( mergePolicy.getMaxMergeMB() ).as( "getMaxMergeMB()" )
								.isCloseTo( LogByteSizeMergePolicy.DEFAULT_MAX_MERGE_MB, withPercentage( 1 ) );
						softly.assertThat( mergePolicy.getMaxMergeMBForForcedMerge() ).as( "getMaxMergeMBForForcedMerge()" )
								// The default exposed by Lucene is in bytes, so we have to convert it
								.isCloseTo( LogByteSizeMergePolicy.DEFAULT_MAX_MERGE_MB_FOR_FORCED_MERGE / 1024 / 1024,
										withPercentage( 1 ) );
						softly.assertThat( mergePolicy.getCalibrateSizeByDeletes() ).as( "getCalibrateSizeByDeletes()" )
								.isTrue();
					} );
				} );

		// Add a document to the index
		IndexIndexingPlan plan = index.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> {} );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Check that writing succeeded
		assertThatQuery( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasTotalHitCount( 1L );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3776")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.configuration.LuceneIndexingParametersTest.testSpecificTypeParametersOverride")
	public void custom() {
		setup( properties -> {
			properties.accept( LuceneIndexSettings.IO_WRITER_MAX_BUFFERED_DOCS, "420" );
			properties.accept( LuceneIndexSettings.IO_WRITER_RAM_BUFFER_SIZE, "420" );
			properties.accept( LuceneIndexSettings.IO_WRITER_INFOSTREAM, "true" );

			properties.accept( LuceneIndexSettings.IO_MERGE_MAX_DOCS, "42000" );
			properties.accept( LuceneIndexSettings.IO_MERGE_FACTOR, "42" );
			properties.accept( LuceneIndexSettings.IO_MERGE_MIN_SIZE, "42" );
			properties.accept( LuceneIndexSettings.IO_MERGE_MAX_SIZE, "420" );
			properties.accept( LuceneIndexSettings.IO_MERGE_MAX_FORCED_SIZE, "42000" );
			properties.accept( LuceneIndexSettings.IO_MERGE_CALIBRATE_BY_DELETES, "false" );
		} );

		LuceneIndexManagerImpl luceneIndexManager = index.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getWriterForTests )
				.extracting( IndexWriter::getConfig )
				.isNotEmpty()
				.allSatisfy( config -> {
					assertSoftly( softly -> {
						softly.assertThat( config.getMaxBufferedDocs() ).as( "getMaxBufferedDocs" )
								.isEqualTo( 420 );
						softly.assertThat( config.getRAMBufferSizeMB() ).as( "getRAMBufferSizeMB" )
								.isEqualTo( 420 );
						softly.assertThat( config.getInfoStream() ).as( "getInfoStream" )
								.isInstanceOf( LoggerInfoStream.class );
					} );

					MergePolicy abstractMergePolicy = config.getMergePolicy();
					assertThat( abstractMergePolicy ).as( "getMergePolicy" ).isNotNull();
					LogByteSizeMergePolicy mergePolicy = (LogByteSizeMergePolicy) abstractMergePolicy;

					assertSoftly( softly -> {
						softly.assertThat( mergePolicy.getMaxMergeDocs() ).as( "getMaxMergeDocs()" )
								.isEqualTo( 42_000 );
						softly.assertThat( mergePolicy.getMergeFactor() ).as( "getMergeFactor()" )
								.isEqualTo( 42 );
						softly.assertThat( mergePolicy.getMinMergeMB() ).as( "getMinMergeMB()" )
								.isCloseTo( 42, withPercentage( 1 ) );
						softly.assertThat( mergePolicy.getMaxMergeMB() ).as( "getMaxMergeMB()" )
								.isCloseTo( 420, withPercentage( 1 ) );
						softly.assertThat( mergePolicy.getMaxMergeMBForForcedMerge() ).as( "getMaxMergeMBForForcedMerge()" )
								.isCloseTo( 42_000, withPercentage( 1 ) );
						softly.assertThat( mergePolicy.getCalibrateSizeByDeletes() ).as( "getCalibrateSizeByDeletes()" )
								.isFalse();
					} );
				} );

		// Add a document to the index
		IndexIndexingPlan plan = index.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> {} );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Check that writing succeeded
		assertThatQuery( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasTotalHitCount( 1L );
	}

	private void setup(Consumer<BiConsumer<String, Object>> propertyContributor) {
		SearchSetupHelper.SetupContext setupContext = setupHelper.start()
				.withIndex( index );
		propertyContributor.accept( setupContext::withBackendProperty );
		setupContext.setup();
	}
}
