/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
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
	public void simple() {
		ElasticsearchWork<?> work1 = work( 1 );
		BulkableElasticsearchWork<?> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1, work2 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor strategy =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( unusedReturnValue() );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		replayAll();
		CompletableFuture<Void> returnedSequenceFuture = strategy.submit( workset1 );
		verifyAll();
		assertThat( returnedSequenceFuture ).isSameAs( sequenceFuture );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = strategy.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();
		sequenceFuture.complete( null );
		assertThat( futureAll ).isSuccessful( (Void) null );
	}

	@Test
	public void parallelSequenceBetweenWorkset() {
		ElasticsearchWork<?> work1 = work( 1 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1 );

		BulkableElasticsearchWork<?> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> workset2 = Arrays.asList( work2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor strategy =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		CompletableFuture<Void> returnedSequence1Future = strategy.submit( workset1 );
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		CompletableFuture<Void> returnedSequence2Future = strategy.submit( workset2 );
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = strategy.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();
		sequence2Future.complete( null );
		assertThat( futureAll ).isPending();
		sequence1Future.complete( null );
		assertThat( futureAll ).isSuccessful( (Void) null );
	}

	@Test
	public void reuseBulkAcrossSequences() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1 );

		BulkableElasticsearchWork<?> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> workset2 = Arrays.asList( work2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor strategy =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		CompletableFuture<Void> returnedSequence1Future = strategy.submit( workset1 );
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		CompletableFuture<Void> returnedSequence2Future = strategy.submit( workset2 );
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = strategy.endBatch();
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
	}

	@Test
	public void newBulkIfNonBulkable_sameWorkset() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		ElasticsearchWork<?> work2 = work( 2 );
		BulkableElasticsearchWork<?> work3 = bulkableWork( 3 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1, work2, work3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor strategy =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.addNonBulkExecution( work2 ) ).andReturn( unusedReturnValue() );
		work3.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work3 ) );
		bulkerMock.finalizeBulkWork();
		expect( bulkerMock.add( work3 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		CompletableFuture<Void> returnedSequence1Future = strategy.submit( workset1 );
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = strategy.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		replayAll();
		sequence1Future.complete( null );
		verifyAll();
		assertThat( futureAll ).isSuccessful( (Void) null );
	}

	@Test
	public void newBulkIfNonBulkable_differentWorksets() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1 );
		ElasticsearchWork<?> work2 = work( 2 );
		BulkableElasticsearchWork<?> work3 = bulkableWork( 3 );
		List<ElasticsearchWork<?>> workset2 = Arrays.asList( work2, work3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchParallelWorkProcessor strategy =
				new ElasticsearchParallelWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		CompletableFuture<Void> returnedSequence1Future = strategy.submit( workset1 );
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.addNonBulkExecution( work2 ) ).andReturn( unusedReturnValue() );
		work3.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work3 ) );
		bulkerMock.finalizeBulkWork();
		expect( bulkerMock.add( work3 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		CompletableFuture<Void> returnedSequence2Future = strategy.submit( workset2 );
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = strategy.endBatch();
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
	}

	private <T> ElasticsearchWork<T> work(int index) {
		return createStrictMock( "work" + index, ElasticsearchWork.class );
	}

	private <T> BulkableElasticsearchWork<T> bulkableWork(int index) {
		return createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
	}

	private IAnswer<Void> nonBulkableAggregateAnswer(ElasticsearchWork<?> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			aggregator.addNonBulkable( mock );
			return null;
		};
	}

	private IAnswer<Void> bulkableAggregateAnswer(BulkableElasticsearchWork<?> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			aggregator.addBulkable( mock );
			return null;
		};
	}
}
