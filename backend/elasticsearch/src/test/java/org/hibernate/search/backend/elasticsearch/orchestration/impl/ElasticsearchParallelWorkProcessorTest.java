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
	public void simple_singleWorkInWorkSet() {
		NonBulkableWork<Object> work = work( 1 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor processor =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		CompletableFuture<Object> workFuture = new CompletableFuture<>();
		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work ) ).andReturn( workFuture );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		replayAll();
		processor.beforeWorkSet();
		CompletableFuture<Object> returnedWork2Future = processor.submit( work );
		processor.afterWorkSet();
		verifyAll();
		assertThat( returnedWork2Future ).isSameAs( workFuture );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();
		sequenceFuture.complete( null );
		assertThat( futureAll ).isSuccessful( (Void) null );

		checkComplete( processor );
	}

	@Test
	public void simple_multipleWorksInWorkSet() {
		NonBulkableWork<Object> work1 = work( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor processor =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( unusedReturnValue() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		processor.submit( work2 );
		CompletableFuture<Void> returnedSequenceFuture = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequenceFuture ).isSameAs( sequenceFuture );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();
		sequenceFuture.complete( null );
		assertThat( futureAll ).isSuccessful( (Void) null );

		checkComplete( processor );
	}

	@Test
	public void parallelSequenceBetweenWorkset() {
		NonBulkableWork<Object> work1 = work( 1 );

		BulkableWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor processor =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		CompletableFuture<Void> returnedSequence1Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work2 );
		CompletableFuture<Void> returnedSequence2Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();
		sequence2Future.complete( null );
		assertThat( futureAll ).isPending();
		sequence1Future.complete( null );
		assertThat( futureAll ).isSuccessful( (Void) null );

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
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		CompletableFuture<Void> returnedSequence1Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work2 );
		CompletableFuture<Void> returnedSequence2Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		replayAll();
		sequence2Future.complete( null );
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		replayAll();
		sequence1Future.complete( null );
		verifyAll();
		assertThat( futureAll ).isSuccessful( (Void) null );

		checkComplete( processor );
	}

	@Test
	public void newBulkIfNonBulkable_sameWorkset() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		NonBulkableWork<Object> work2 = work( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor processor =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.addNonBulkExecution( work2 ) ).andReturn( unusedReturnValue() );
		expect( work3.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work3 ) );
		bulkerMock.finalizeBulkWork();
		expect( bulkerMock.add( work3 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		processor.submit( work2 );
		processor.submit( work3 );
		CompletableFuture<Void> returnedSequence1Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		replayAll();
		sequence1Future.complete( null );
		verifyAll();
		assertThat( futureAll ).isSuccessful( (Void) null );

		checkComplete( processor );
	}

	@Test
	public void newBulkIfNonBulkable_differentWorksets() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		NonBulkableWork<Object> work2 = work( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor processor =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		CompletableFuture<Void> returnedSequence1Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.addNonBulkExecution( work2 ) ).andReturn( unusedReturnValue() );
		expect( work3.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work3 ) );
		bulkerMock.finalizeBulkWork();
		expect( bulkerMock.add( work3 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work2 );
		processor.submit( work3 );
		CompletableFuture<Void> returnedSequence2Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		replayAll();
		sequence2Future.complete( null );
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		replayAll();
		sequence1Future.complete( null );
		verifyAll();
		assertThat( futureAll ).isSuccessful( (Void) null );

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
