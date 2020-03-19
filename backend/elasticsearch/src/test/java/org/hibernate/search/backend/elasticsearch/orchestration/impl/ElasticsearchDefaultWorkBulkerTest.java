/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

import org.junit.Before;
import org.junit.Test;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;


public class ElasticsearchDefaultWorkBulkerTest extends EasyMockSupport {

	private static final int DEFAULT_MAX_BULK_SIZE = 10;

	private static final DocumentRefreshStrategy DEFAULT_REFRESH = DocumentRefreshStrategy.NONE;

	private ElasticsearchWorkSequenceBuilder sequenceBuilderMock;
	private ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep bulkResultExtractionStepMock;
	private BiFunction<List<? extends BulkableWork<?>>, DocumentRefreshStrategy, NonBulkableWork<BulkResult>> bulkWorkFactoryMock;

	@Before
	public void initMocks() {
		sequenceBuilderMock = createStrictMock( ElasticsearchWorkSequenceBuilder.class );
		bulkResultExtractionStepMock = createStrictMock( BulkResultExtractionStep.class );
		bulkWorkFactoryMock = createStrictMock( BiFunction.class );
	}

	@Test
	public void simple() {
		BulkableWork<Void> work1 = bulkableWork( 1 );
		BulkableWork<Void> work2 = bulkableWork( 2 );
		NonBulkableWork<BulkResult> bulkWork = work( 3 );

		CompletableFuture<Void> work1Future;
		CompletableFuture<Void> work1FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work2Future;
		CompletableFuture<Void> work2FutureFromSequenceBuilder = new CompletableFuture<>();
		Capture<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWorkFutureCapture = newCapture();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock, DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		expect( work1.getRefreshStrategy() ).andReturn( DEFAULT_REFRESH );
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWorkFutureCapture ) ) ).andReturn( bulkWorkResultFuture );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( work1, 0 ) ).andReturn( work1FutureFromSequenceBuilder );
		replayAll();
		work1Future = bulker.add( work1 );
		verifyAll();
		assertThat( work1Future ).isPending();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		resetAll();
		expect( work2.getRefreshStrategy() ).andReturn( DEFAULT_REFRESH );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( work2, 1 ) ).andReturn( work2FutureFromSequenceBuilder );
		replayAll();
		work2Future = bulker.add( work2 );
		verifyAll();
		assertThat( work1Future ).isPending();
		assertThat( work2Future ).isPending();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2 ), DEFAULT_REFRESH ) ).andReturn( bulkWork );
		replayAll();
		bulker.finalizeBulkWork();
		verifyAll();
		assertThat( work1Future ).isPending();
		assertThat( work2Future ).isPending();
		assertThat( bulkWorkFutureCapture.getValue() ).isSuccessful( bulkWork );

		// Check that per-work futures are correctly bound to the futures returned by the sequence builder
		resetAll();
		replayAll();
		work1FutureFromSequenceBuilder.complete( null );
		assertThat( work1Future ).isSuccessful( (Void) null );
		work2FutureFromSequenceBuilder.completeExceptionally( new RuntimeException() );
		assertThat( work2Future ).isFailed();
		verifyAll();
	}

	@Test
	public void alwaysBulk() {
		BulkableWork<Void> work1 = bulkableWork( 1 );
		NonBulkableWork<BulkResult> bulkWork = work( 2 );

		CompletableFuture<Void> work1FutureFromSequenceBuilder = new CompletableFuture<>();
		Capture<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWorkFutureCapture = newCapture();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock, DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		expect( work1.getRefreshStrategy() ).andReturn( DEFAULT_REFRESH );
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWorkFutureCapture ) ) ).andReturn( bulkWorkResultFuture );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( work1, 0 ) ).andReturn( work1FutureFromSequenceBuilder );
		replayAll();
		bulker.add( work1 );
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1 ), DEFAULT_REFRESH ) ).andReturn( bulkWork );
		replayAll();
		bulker.finalizeBulkWork();
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isSuccessful( bulkWork );
	}

	@Test
	public void newBulkOnTooManyBulkedWorks() {
		List<BulkableWork<Void>> firstBulkWorks = new ArrayList<>();
		for ( int i = 0 ; i < DEFAULT_MAX_BULK_SIZE ; ++i ) {
			firstBulkWorks.add( bulkableWork( i ) );
		}
		BulkableWork<Void> additionalWork1 = bulkableWork( DEFAULT_MAX_BULK_SIZE );
		BulkableWork<Void> additionalWork2 = bulkableWork( DEFAULT_MAX_BULK_SIZE + 1 );
		NonBulkableWork<BulkResult> bulkWork1 = work( DEFAULT_MAX_BULK_SIZE + 2 );
		NonBulkableWork<BulkResult> bulkWork2 = work( DEFAULT_MAX_BULK_SIZE + 3 );

		List<CompletableFuture<Void>> firstBulkWorksCompletableFuturesFromSequenceBuilder = new ArrayList<>();
		for ( int i = 0 ; i < DEFAULT_MAX_BULK_SIZE ; ++i ) {
			firstBulkWorksCompletableFuturesFromSequenceBuilder.add( new CompletableFuture<>() );
		}
		CompletableFuture<Void> additionalWork1FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> additionalWork2FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork1ResultFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork2ResultFuture = new CompletableFuture<>();
		Capture<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork1FutureCapture = newCapture();
		Capture<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork2FutureCapture = newCapture();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock, DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork1FutureCapture ) ) ).andReturn( bulkWork1ResultFuture );
		for ( int i = 0 ; i < DEFAULT_MAX_BULK_SIZE ; ++i ) {
			BulkableWork<Void> work = firstBulkWorks.get( i );
			expect( work.getRefreshStrategy() ).andReturn( DEFAULT_REFRESH );
			expect( sequenceBuilderMock.addBulkResultExtraction( bulkWork1ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
			expect( bulkResultExtractionStepMock.add( work, i ) )
					.andReturn( firstBulkWorksCompletableFuturesFromSequenceBuilder.get( i ) );
		}
		expect( bulkWorkFactoryMock.apply( firstBulkWorks, DEFAULT_REFRESH ) ).andReturn( bulkWork1 );
		replayAll();
		for ( BulkableWork<?> work : firstBulkWorks ) {
			bulker.add( work );
		}
		verifyAll();
		assertThat( bulkWork1FutureCapture.getValue() ).isSuccessful( bulkWork1 );

		resetAll();
		expect( additionalWork1.getRefreshStrategy() ).andReturn( DEFAULT_REFRESH );
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork2FutureCapture ) ) ).andReturn( bulkWork2ResultFuture );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWork2ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( additionalWork1, 0 ) ).andReturn( additionalWork1FutureFromSequenceBuilder );
		replayAll();
		bulker.add( additionalWork1 );
		verifyAll();
		assertThat( bulkWork2FutureCapture.getValue() ).isPending();

		resetAll();
		expect( additionalWork2.getRefreshStrategy() ).andReturn( DEFAULT_REFRESH );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWork2ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( additionalWork2, 1 ) ).andReturn( additionalWork2FutureFromSequenceBuilder );
		replayAll();
		bulker.add( additionalWork2 );
		verifyAll();
		assertThat( bulkWork2FutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( additionalWork1, additionalWork2 ), DEFAULT_REFRESH ) ).andReturn(
				bulkWork2 );
		replayAll();
		bulker.finalizeBulkWork();
		verifyAll();
		assertThat( bulkWork2FutureCapture.getValue() ).isSuccessful( bulkWork2 );
	}

	@Test
	public void newBulkOnDifferentRefresh() {
		BulkableWork<Void> work1 = bulkableWork( 1 );
		BulkableWork<Void> work2 = bulkableWork( 2 );
		BulkableWork<Void> work3 = bulkableWork( 3 );
		BulkableWork<Void> work4 = bulkableWork( 4 );
		BulkableWork<Void> work5 = bulkableWork( 5 );
		NonBulkableWork<BulkResult> bulkWork1 = work( 7 );
		NonBulkableWork<BulkResult> bulkWork2 = work( 8 );
		NonBulkableWork<BulkResult> bulkWork3 = work( 9 );

		CompletableFuture<Void> work1FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work2FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work3FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work4FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work5FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork1ResultFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork2ResultFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork3ResultFuture = new CompletableFuture<>();
		Capture<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork1FutureCapture = newCapture();
		Capture<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork2FutureCapture = newCapture();
		Capture<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork3FutureCapture = newCapture();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock, 10 );
		verifyAll();

		resetAll();
		expect( work1.getRefreshStrategy() ).andReturn( DocumentRefreshStrategy.NONE );
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork1FutureCapture ) ) ).andReturn( bulkWork1ResultFuture );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWork1ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( work1, 0 ) ).andReturn( work1FutureFromSequenceBuilder );
		replayAll();
		bulker.add( work1 );
		verifyAll();

		resetAll();
		expect( work2.getRefreshStrategy() ).andReturn( DocumentRefreshStrategy.NONE );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWork1ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( work2, 1 ) ).andReturn( work2FutureFromSequenceBuilder );
		replayAll();
		bulker.add( work2 );
		verifyAll();

		// ForceRefresh from NONE to FORCE => new bulk
		resetAll();
		expect( work3.getRefreshStrategy() ).andReturn( DocumentRefreshStrategy.FORCE );
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2 ), DocumentRefreshStrategy.NONE ) ).andReturn(
				bulkWork1 );
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork2FutureCapture ) ) ).andReturn( bulkWork2ResultFuture );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWork2ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( work3, 0 ) ).andReturn( work3FutureFromSequenceBuilder );
		replayAll();
		bulker.add( work3 );
		verifyAll();
		assertThat( bulkWork1FutureCapture.getValue() ).isSuccessful( bulkWork1 );

		resetAll();
		expect( work4.getRefreshStrategy() ).andReturn( DocumentRefreshStrategy.FORCE );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWork2ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( work4, 1 ) ).andReturn( work4FutureFromSequenceBuilder );
		replayAll();
		bulker.add( work4 );
		verifyAll();

		// ForceRefresh from FORCE to NONE => new bulk
		resetAll();
		expect( work5.getRefreshStrategy() ).andReturn( DocumentRefreshStrategy.NONE );
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work3, work4 ), DocumentRefreshStrategy.FORCE ) ).andReturn(
				bulkWork2 );
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork3FutureCapture ) ) ).andReturn( bulkWork3ResultFuture );
		expect( sequenceBuilderMock.addBulkResultExtraction( bulkWork3ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		expect( bulkResultExtractionStepMock.add( work5, 0 ) ).andReturn( work5FutureFromSequenceBuilder );
		replayAll();
		bulker.add( work5 );
		verifyAll();
		assertThat( bulkWork2FutureCapture.getValue() ).isSuccessful( bulkWork2 );

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work5 ), DocumentRefreshStrategy.NONE ) ).andReturn(
				bulkWork3 );
		replayAll();
		bulker.finalizeBulkWork();
		verifyAll();
	}

	private <T> NonBulkableWork<T> work(int index) {
		return createStrictMock( "work" + index, NonBulkableWork.class );
	}

	private <T> BulkableWork<T> bulkableWork(int index) {
		return createStrictMock( "bulkableWork" + index, BulkableWork.class );
	}
}
