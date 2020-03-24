/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;

import org.junit.Before;
import org.junit.Test;

import org.easymock.EasyMockSupport;


public class ElasticsearchBatchedWorkProcessorTest extends EasyMockSupport {

	/**
	 * @return A value that should not matter, because it should not be used.
	 */
	private static <T> T unusedReturnValue() {
		return null;
	}

	private ElasticsearchWorkSequenceBuilder sequenceBuilderMock;
	private ElasticsearchWorkBulker bulkerMock;

	@Before
	public void initMocks() {
		sequenceBuilderMock = createStrictMock( ElasticsearchWorkSequenceBuilder.class );
		bulkerMock = createStrictMock( ElasticsearchWorkBulker.class );
	}

	@Test
	public void simple_singleWork() {
		BulkableWork<Object> work = bulkableWork( 1 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchBatchedWorkProcessor processor =
				new ElasticsearchBatchedWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		sequenceBuilderMock.init( anyObject() );
		replayAll();
		processor.beginBatch();
		verifyAll();

		CompletableFuture<Object> workFuture = new CompletableFuture<>();
		resetAll();
		expect( bulkerMock.add( work ) ).andReturn( workFuture );
		replayAll();
		CompletableFuture<Object> returnedWorkFuture = processor.submit( work );
		verifyAll();
		assertThat( returnedWorkFuture ).isSameAs( workFuture );

		resetAll();
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> batchFuture = processor.endBatch();
		verifyAll();
		assertThat( batchFuture ).isPending();

		resetAll();
		sequenceFuture.complete( null );
		replayAll();
		assertThat( batchFuture ).isSuccessful();
		verifyAll();

		checkComplete( processor );
	}

	@Test
	public void simple_multipleWorks() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchBatchedWorkProcessor processor =
				new ElasticsearchBatchedWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		sequenceBuilderMock.init( anyObject() );
		replayAll();
		processor.beginBatch();
		verifyAll();

		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		resetAll();
		expect( bulkerMock.add( work1 ) ).andReturn( work1Future );
		expect( bulkerMock.add( work2 ) ).andReturn( work2Future );
		replayAll();
		CompletableFuture<Object> returnedWork1Future = processor.submit( work1 );
		CompletableFuture<Object> returnedWork2Future = processor.submit( work2 );
		verifyAll();
		assertThat( returnedWork1Future ).isSameAs( work1Future );
		assertThat( returnedWork2Future ).isSameAs( work2Future );

		resetAll();
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> batchFuture = processor.endBatch();
		verifyAll();
		assertThat( batchFuture ).isPending();

		resetAll();
		sequenceFuture.complete( null );
		replayAll();
		assertThat( batchFuture ).isSuccessful();

		checkComplete( processor );
	}

	@Test
	public void newSequenceBetweenBatches() {
		BulkableWork<Object> work1 = bulkableWork( 1 );

		BulkableWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchBatchedWorkProcessor processor =
				new ElasticsearchBatchedWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		sequenceBuilderMock.init( anyObject() );
		replayAll();
		processor.beginBatch();
		verifyAll();

		resetAll();
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		replayAll();
		processor.submit( work1 );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> batch1Future = processor.endBatch();
		verifyAll();
		assertThat( batch1Future ).isPending();

		resetAll();
		sequence1Future.complete( null );
		replayAll();
		assertThat( batch1Future ).isSuccessful();

		resetAll();
		bulkerMock.reset();
		sequenceBuilderMock.init( anyObject() );
		replayAll();
		processor.beginBatch();
		verifyAll();

		resetAll();
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		replayAll();
		processor.submit( work2 );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> batch2Future = processor.endBatch();
		verifyAll();
		assertThat( batch2Future ).isPending();

		resetAll();
		sequence2Future.complete( null );
		replayAll();
		assertThat( batch2Future ).isSuccessful();

		checkComplete( processor );
	}

	private void checkComplete(ElasticsearchBatchedWorkProcessor processor) {
		resetAll();
		replayAll();
		processor.complete();
		verifyAll();
	}

	private <T> BulkableWork<T> bulkableWork(int index) {
		return createStrictMock( "bulkableWork" + index, BulkableWork.class );
	}
}
