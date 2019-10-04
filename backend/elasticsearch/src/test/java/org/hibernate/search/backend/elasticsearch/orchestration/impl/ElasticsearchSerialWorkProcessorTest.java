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
	public void simple() {
		ElasticsearchWork<Object> work1 = work( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1, work2 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
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
		CompletableFuture<Void> returnedSequenceFuture = processor.submit( workset1 );
		verifyAll();
		assertThat( returnedSequenceFuture ).isSameAs( sequenceFuture );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isSameAs( sequenceFuture );
	}

	@Test
	public void newSequenceBetweenWorkset() {
		ElasticsearchWork<Object> work1 = work( 1 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1 );

		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> workset2 = Arrays.asList( work2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		CompletableFuture<Void> returnedSequence1Future = processor.submit( workset1 );
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( sequence1Future );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		CompletableFuture<Void> returnedSequence2Future = processor.submit( workset2 );
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isSameAs( sequence2Future );
	}

	@Test
	public void reuseBulkAcrossSequences() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1 );

		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> workset2 = Arrays.asList( work2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		CompletableFuture<Void> returnedSequence1Future = processor.submit( workset1 );
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( sequence1Future );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		CompletableFuture<Void> returnedSequence2Future = processor.submit( workset2 );
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isSameAs( sequence2Future );
	}

	@Test
	public void newBulkIfNonBulkable_sameWorkset() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		ElasticsearchWork<Object> work2 = work( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1, work2, work3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
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
		CompletableFuture<Void> returnedSequence1Future = processor.submit( workset1 );
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isSameAs( sequence1Future );
	}

	@Test
	public void newBulkIfNonBulkable_differentWorksets() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		List<ElasticsearchWork<?>> workset1 = Arrays.asList( work1 );
		ElasticsearchWork<Object> work2 = work( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		List<ElasticsearchWork<?>> workset2 = Arrays.asList( work2, work3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		CompletableFuture<Void> returnedSequence1Future = processor.submit( workset1 );
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
		CompletableFuture<Void> returnedSequence2Future = processor.submit( workset2 );
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isSameAs( sequence2Future );
	}

	private <T> ElasticsearchWork<T> work(int index) {
		return createStrictMock( "work" + index, ElasticsearchWork.class );
	}

	private <T> BulkableElasticsearchWork<T> bulkableWork(int index) {
		return createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
	}

	private <T> IAnswer<CompletableFuture<T>> nonBulkableAggregateAnswer(ElasticsearchWork<T> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			return aggregator.addNonBulkable( mock );
		};
	}

	private <T> IAnswer<CompletableFuture<T>> bulkableAggregateAnswer(BulkableElasticsearchWork<T> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			return aggregator.addBulkable( mock );
		};
	}
}
