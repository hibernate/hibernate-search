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


public class ElasticsearchParallelWorkProcessorTest extends EasyMockSupport {

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
		ElasticsearchParallelWorkProcessor processor =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		replayAll();
		processor.beginBatch();
		verifyAll();

		CompletableFuture<Object> workFuture = new CompletableFuture<>();
		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work ) );
		expect( sequenceBuilderMock.addNonBulkExecution( work ) ).andReturn( workFuture );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		replayAll();
		CompletableFuture<Object> returnedWorkFuture = processor.submit( work );
		verifyAll();
		assertThat( returnedWorkFuture ).isSameAs( workFuture );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> batchFuture = processor.endBatch();
		verifyAll();
		assertThat( batchFuture ).isPending();

		resetAll();
		replayAll();
		sequenceFuture.complete( null );
		verifyAll();
		assertThat( batchFuture ).isSuccessful();

		checkComplete( processor );
	}

	@Test
	public void simple_multipleWorks() {
		NonBulkableWork<Object> work1 = work( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor processor =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		replayAll();
		processor.beginBatch();
		verifyAll();

		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( work1Future );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		sequenceBuilderMock.init( anyObject() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( work2Future );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		CompletableFuture<Object> returnedWork1Future = processor.submit( work1 );
		CompletableFuture<Object> returnedWork2Future = processor.submit( work2 );
		verifyAll();
		assertThat( returnedWork1Future ).isSameAs( work1Future );
		assertThat( returnedWork2Future ).isSameAs( work2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> batchFuture = processor.endBatch();
		verifyAll();
		assertThat( batchFuture ).isPending();

		resetAll();
		replayAll();
		sequence2Future.complete( null );
		verifyAll();
		assertThat( batchFuture ).isPending();

		resetAll();
		replayAll();
		sequence1Future.complete( null );
		verifyAll();
		assertThat( batchFuture ).isSuccessful();

		checkComplete( processor );
	}

	@Test
	public void reuseBulkAcrossSequences() {
		BulkableWork<Object> work1 = bulkableWork( 1 );

		BulkableWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor processor =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		bulkerMock.reset();
		replayAll();
		processor.beginBatch();
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.submit( work1 );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		processor.submit( work2 );
		verifyAll();

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> batchFuture = processor.endBatch();
		verifyAll();
		assertThat( batchFuture ).isPending();

		resetAll();
		replayAll();
		sequence2Future.complete( null );
		verifyAll();
		assertThat( batchFuture ).isPending();

		resetAll();
		replayAll();
		sequence1Future.complete( null );
		verifyAll();
		assertThat( batchFuture ).isSuccessful();

		checkComplete( processor );
	}

	private void checkComplete(ElasticsearchParallelWorkProcessor processor) {
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
