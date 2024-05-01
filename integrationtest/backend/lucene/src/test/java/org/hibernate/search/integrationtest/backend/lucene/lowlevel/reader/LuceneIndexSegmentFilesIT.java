/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StandardDirectoryReader;
import org.assertj.core.api.SoftAssertionsProvider.ThrowingRunnable;
import org.awaitility.Awaitility;

class LuceneIndexSegmentFilesIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	// we are using a "unique" index name to omit any other tests writing to the same directory
	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
			.name( LuceneIndexSegmentFilesIT.class.getName() );


	@Test
	void merge() throws Exception {

		basicTestSteps( () -> {
			// now let's start scrolling so that we are going to lock some of the to-be-stale segments:
			SearchScroll<DocumentReference> scroll = index.query().where( f -> f.matchAll() ).scroll( 5 );
			SearchScrollResult<DocumentReference> next = scroll.next();
			assertThat( next.hits() ).hasSize( 5 );

			IndexWorkspace workspace = index.createWorkspace();
			workspace.mergeSegments( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();

			// we've merged the segments, so we expect that there's exactly 1 current segment
			assertThat( numberOfCurrentSegments() ).isEqualTo( 1 );
			// we do have a scroll opened so there are some stale files:
			assertThat( numberOfAllSegments() ).isGreaterThan( 1 );
			// scroll still works:
			next = scroll.next();
			assertThat( next.hits() ).hasSize( 5 );

			// now we are going to close the scroll releasing any stale files:
			scroll.close();
			assertThat( numberOfCurrentSegments() ).isEqualTo( 1 );
			Awaitility.await()
					.timeout( 10, TimeUnit.SECONDS )
					.untilAsserted( () -> assertThat( numberOfAllSegments() ).isEqualTo( 1 ) );
		} );
	}

	@Test
	void purge() throws Exception {

		basicTestSteps( () -> {
			// now let's start scrolling so that we are going to lock some of the to-be-stale segments:
			SearchScroll<DocumentReference> scroll = index.query().where( f -> f.matchAll() ).scroll( 5 );
			SearchScrollResult<DocumentReference> next = scroll.next();
			assertThat( next.hits() ).hasSize( 5 );

			IndexWorkspace workspace = index.createWorkspace();
			workspace.purge( Set.of(), OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();

			// we've purged the segments, so we expect that there's exactly 0 current segment
			assertThat( numberOfCurrentSegments() ).isZero();
			// we do have a scroll opened so there are some stale files:
			assertThat( numberOfAllSegments() ).isGreaterThan( 1 );
			// scroll still works:
			next = scroll.next();
			assertThat( next.hits() ).hasSize( 5 );

			// now we are going to close the scroll releasing any stale files:
			scroll.close();
			assertThat( numberOfCurrentSegments() ).isZero();
			Awaitility.await()
					.timeout( 10, TimeUnit.SECONDS )
					.untilAsserted( () -> assertThat( numberOfAllSegments() ).isZero() );
		} );
	}


	private void basicTestSteps(ThrowingRunnable action) throws Exception {

		int totalDocs = 20;

		createSomeInitialDocuments( totalDocs );

		try ( StubMapping mapping = setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( index )
				.setup() ) {
			// There's still some segments after index is reopened:
			long numberOfCurrentSegments = numberOfCurrentSegments();
			assertThat( numberOfCurrentSegments ).isGreaterThanOrEqualTo( 1 );
			// And there's no stale segments left since if even any were there before closing the previous IndexWriter
			//  those were supposed to be cleaned up:
			assertThat( numberOfAllSegments() ).isEqualTo( numberOfCurrentSegments );

			// Let's add more documents so that other segment files are created
			createMoreDocuments( totalDocs );

			action.run();
		}
	}

	private void createMoreDocuments(int totalDocs) throws IOException {
		createSomeDocuments( totalDocs, totalDocs );
		assertThatQuery( index.query().where( SearchPredicateFactory::matchAll ) ).hasTotalHitCount( 2 * totalDocs );
		// we expect that there's +1 segment, but let's be more forgiving and say that there's at least +1
		assertThat( numberOfCurrentSegments() ).isGreaterThanOrEqualTo( 1 );
		assertThat( numberOfAllSegments() ).isGreaterThan( 1 );
	}

	private void createSomeInitialDocuments(int totalDocs) throws IOException {
		try ( StubMapping mapping = setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY )
				.withIndex( index )
				.setup() ) {
			assertThatQuery( index.query().where( SearchPredicateFactory::matchAll ) ).hasNoHits();
			assertThat( numberOfCurrentSegments() ).isEqualTo( 0 );

			createSomeDocuments( 0, totalDocs );

			assertThatQuery( index.query().where( SearchPredicateFactory::matchAll ) ).hasTotalHitCount( totalDocs );
			assertThat( numberOfCurrentSegments() ).isGreaterThanOrEqualTo( 1 );
			assertThat( numberOfAllSegments() ).isGreaterThanOrEqualTo( 1 );
		}
	}

	private long numberOfCurrentSegments() throws IOException {
		try ( IndexReader indexReader = index.createScope().extension( LuceneExtension.get() ).openIndexReader() ) {
			long segments = 0;
			for ( StandardDirectoryReader reader : getStandardDirectoryReaders( indexReader ) ) {
				segments += reader.getSegmentInfos().size();
			}
			return segments;
		}
	}

	private long numberOfAllSegments() throws IOException {
		try ( IndexReader indexReader = index.createScope().extension( LuceneExtension.get() ).openIndexReader() ) {
			long segments = 0;
			for ( StandardDirectoryReader reader : getStandardDirectoryReaders( indexReader ) ) {
				// Segment Info	.si	Stores metadata about a segment
				segments += Arrays.stream( reader.directory().listAll() ).filter( file -> file.endsWith( ".si" ) ).count();
			}
			return segments;
		}
	}

	private void createSomeDocuments(int start, int totalDocs) {
		BulkIndexer bulkIndexer = index.bulkIndexer();
		for ( int i = start; i < start + totalDocs; i++ ) {
			String value = "value_" + i;
			bulkIndexer.add( "id:" + i, c -> c.addValue( "string", value ) );
		}
		bulkIndexer.join();
	}

	@SuppressWarnings("unchecked")
	private List<StandardDirectoryReader> getStandardDirectoryReaders(IndexReader indexReader) {
		return indexReader.getContext().children().stream()
				.map( ctx -> (StandardDirectoryReader) ctx.reader() )
				.collect( Collectors.toList() );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> vectorField;

		IndexBinding(IndexSchemaElement root) {
			vectorField = root.field( "string", c -> c.asString() ).toReference();
		}
	}
}
