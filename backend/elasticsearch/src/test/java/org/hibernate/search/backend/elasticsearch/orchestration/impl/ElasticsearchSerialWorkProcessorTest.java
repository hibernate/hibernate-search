/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkAggregator;

import org.junit.Before;
import org.junit.Test;

import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;


public class ElasticsearchSerialWorkProcessorTest extends EasyMockSupport {

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
		NonBulkableWork<Object> work = work( 1 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		sequenceBuilderMock.init( anyObject() );
		replayAll();
		processor.beginBatch();
		verifyAll();

		CompletableFuture<Object> workFuture = new CompletableFuture<>();
		resetAll();
		expect( work.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work ) );
		expect( sequenceBuilderMock.addNonBulkExecution( work ) ).andReturn( workFuture );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		replayAll();
		CompletableFuture<Object> returnedWorkFuture = processor.submit( work );
		verifyAll();
		assertThat( returnedWorkFuture ).isSameAs( workFuture );

		resetAll();
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
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
		NonBulkableWork<Object> work1 = work( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
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
		expect( work1.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( work1Future );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( work2Future );
		replayAll();
		CompletableFuture<Object> returnedWork1Future = processor.submit( work1 );
		CompletableFuture<Object> returnedWork2Future = processor.submit( work2 );
		verifyAll();
		assertThat( returnedWork1Future ).isSameAs( work1Future );
		assertThat( returnedWork2Future ).isSameAs( work2Future );

		resetAll();
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
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
		NonBulkableWork<Object> work1 = work( 1 );

		BulkableWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		sequenceBuilderMock.init( anyObject() );
		replayAll();
		processor.beginBatch();
		verifyAll();

		resetAll();
		expect( work1.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( unusedReturnValue() );
		replayAll();
		processor.submit( work1 );
		verifyAll();

		resetAll();
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
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
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		replayAll();
		processor.submit( work2 );
		verifyAll();

		resetAll();
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
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

	@Test
	public void newBulkIfNonBulkable() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		NonBulkableWork<Object> work2 = work( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		sequenceBuilderMock.init( anyObject() );
		replayAll();
		processor.beginBatch();
		verifyAll();

		resetAll();
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		// New bulk because a non-bulkable work was encountered
		bulkerMock.finalizeBulkWork();
		expect( sequenceBuilderMock.addNonBulkExecution( work2 ) ).andReturn( unusedReturnValue() );
		expect( work3.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work3 ) );
		expect( bulkerMock.add( work3 ) ).andReturn( unusedReturnValue() );
		replayAll();
		processor.submit( work1 );
		processor.submit( work2 );
		processor.submit( work3 );
		verifyAll();

		resetAll();
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
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

	private void checkComplete(ElasticsearchSerialWorkProcessor processor) {
		resetAll();
		replayAll();
		processor.complete();
		verifyAll();
	}

	private <T> NonBulkableWork<T> work(int index) {
		return createStrictMock( "work" + index, NonBulkableWork.class );
	}

	private <T> BulkableWork<T> bulkableWork(int index) {
		return createStrictMock( "bulkableWork" + index, BulkableWork.class );
	}

	private <T> IAnswer<CompletableFuture<T>> nonBulkableAggregateAnswer(NonBulkableWork<T> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			return aggregator.addNonBulkable( mock );
		};
	}

	private <T> IAnswer<CompletableFuture<T>> bulkableAggregateAnswer(BulkableWork<T> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			return aggregator.addBulkable( mock );
		};
	}
}
