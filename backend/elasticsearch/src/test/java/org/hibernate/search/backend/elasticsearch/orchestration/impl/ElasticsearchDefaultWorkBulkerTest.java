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
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;

import org.junit.Before;
import org.junit.Test;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchDefaultWorkBulkerTest extends EasyMockSupport {

	private static final int DEFAULT_MIN_BULK_SIZE = 1;

	private static final int DEFAULT_MAX_BULK_SIZE = 10;

	private ElasticsearchWorkSequenceBuilder sequenceBuilderMock;
	private ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep bulkResultExtractionStepMock;
	private Function<List<BulkableElasticsearchWork<?>>, ElasticsearchWork<BulkResult>> bulkWorkFactoryMock;

	@Before
	public void initMocks() {
		sequenceBuilderMock = createStrictMock( ElasticsearchWorkSequenceBuilder.class );
		bulkResultExtractionStepMock = createStrictMock( BulkResultExtractionStep.class );
		bulkWorkFactoryMock = createStrictMock( Function.class );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void simple() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		ElasticsearchWork<BulkResult> bulkWork = work( 3 );

		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWorkFutureCapture = newCapture();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						DEFAULT_MIN_BULK_SIZE, DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		replayAll();
		bulker.add( work1 );
		verifyAll();

		resetAll();
		replayAll();
		bulker.add( work2 );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWorkFutureCapture ) ) ).andReturn( bulkWorkResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work1, 0 );
		bulkResultExtractionStepMock.add( work2, 1 );
		replayAll();
		bulker.flushBulked();
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2 ) ) ).andReturn( (ElasticsearchWork) bulkWork );
		replayAll();
		bulker.flushBulk();
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isSuccessful( bulkWork );
	}

	@Test
	public void noBulkIfBelowThreshold() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						2 /* Mandate minimum 2 works per bulk */,
						DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		replayAll();
		bulker.add( work1 );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( new CompletableFuture<>() );
		replayAll();
		bulker.flushBulked();
		verifyAll();

		resetAll();
		replayAll();
		bulker.flushBulk();
		verifyAll();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void alwaysBulkIfAboveThreshold() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		ElasticsearchWork<BulkResult> bulkWork = work( 2 );

		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWorkFutureCapture = newCapture();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						1 /* No threshold, even 1 work per bulk is okay */,
						DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		replayAll();
		bulker.add( work1 );
		verifyAll();


		resetAll();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWorkFutureCapture ) ) ).andReturn( bulkWorkResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work1, 0 );
		replayAll();
		bulker.flushBulked();
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1 ) ) ).andReturn( (ElasticsearchWork) bulkWork );
		replayAll();
		bulker.flushBulk();
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isSuccessful( bulkWork );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void sameBulkOnFlushBulked() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<Void> work4 = bulkableWork( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWorkFutureCapture = newCapture();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						DEFAULT_MIN_BULK_SIZE, DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		replayAll();
		bulker.add( work1 );
		bulker.add( work2 );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWorkFutureCapture ) ) ).andReturn( bulkWorkResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work1, 0 );
		bulkResultExtractionStepMock.add( work2, 1 );
		replayAll();
		bulker.flushBulked();
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		resetAll();
		replayAll();
		bulker.add( work3 );
		bulker.add( work4 );
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		resetAll();
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work3, 2 );
		bulkResultExtractionStepMock.add( work4, 3 );
		replayAll();
		bulker.flushBulked();
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2, work3, work4 ) ) ).andReturn( (ElasticsearchWork) bulkWork );
		replayAll();
		bulker.flushBulk();
		verifyAll();
		assertThat( bulkWorkFutureCapture.getValue() ).isSuccessful( bulkWork );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void newBulkOnFlushBulk() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<Void> work4 = bulkableWork( 4 );
		ElasticsearchWork<BulkResult> bulkWork1 = work( 5 );
		ElasticsearchWork<BulkResult> bulkWork2 = work( 6 );

		CompletableFuture<BulkResult> bulkWork1ResultFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork2ResultFuture = new CompletableFuture<>();
		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWork1FutureCapture = newCapture();
		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWork2FutureCapture = newCapture();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						DEFAULT_MIN_BULK_SIZE, DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		replayAll();
		bulker.add( work1 );
		bulker.add( work2 );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork1FutureCapture ) ) ).andReturn( bulkWork1ResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWork1ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work1, 0 );
		bulkResultExtractionStepMock.add( work2, 1 );
		replayAll();
		bulker.flushBulked();
		verifyAll();
		assertThat( bulkWork1FutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2 ) ) ).andReturn( (ElasticsearchWork) bulkWork1 );
		replayAll();
		bulker.flushBulk();
		verifyAll();
		assertThat( bulkWork1FutureCapture.getValue() ).isSuccessful( bulkWork1 );

		resetAll();
		replayAll();
		bulker.add( work3 );
		bulker.add( work4 );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork2FutureCapture ) ) ).andReturn( bulkWork2ResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWork2ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work3, 0 );
		bulkResultExtractionStepMock.add( work4, 1 );
		replayAll();
		bulker.flushBulked();
		verifyAll();
		assertThat( bulkWork2FutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work3, work4 ) ) ).andReturn( (ElasticsearchWork) bulkWork2 );
		replayAll();
		bulker.flushBulk();
		verifyAll();
		assertThat( bulkWork2FutureCapture.getValue() ).isSuccessful( bulkWork2 );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void newBulkOnTooManyBulkedWorks() {
		List<BulkableElasticsearchWork<?>> firstBulkWorks = new ArrayList<>();
		for ( int i = 0 ; i < DEFAULT_MAX_BULK_SIZE ; ++i ) {
			firstBulkWorks.add( bulkableWork( i ) );
		}
		BulkableElasticsearchWork<Void> additionalWork1 = bulkableWork( DEFAULT_MAX_BULK_SIZE );
		BulkableElasticsearchWork<Void> additionalWork2 = bulkableWork( DEFAULT_MAX_BULK_SIZE + 1 );
		ElasticsearchWork<BulkResult> bulkWork1 = work( DEFAULT_MAX_BULK_SIZE + 2 );
		ElasticsearchWork<BulkResult> bulkWork2 = work( DEFAULT_MAX_BULK_SIZE + 3 );

		CompletableFuture<BulkResult> bulkWork1ResultFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork2ResultFuture = new CompletableFuture<>();
		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWork1FutureCapture = newCapture();
		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWork2FutureCapture = newCapture();

		replayAll();
		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						DEFAULT_MIN_BULK_SIZE, DEFAULT_MAX_BULK_SIZE );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork1FutureCapture ) ) ).andReturn( bulkWork1ResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWork1ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		for ( int i = 0 ; i < DEFAULT_MAX_BULK_SIZE ; ++i ) {
			BulkableElasticsearchWork<?> work = firstBulkWorks.get( i );
			bulkResultExtractionStepMock.add( work, i );
		}
		expect( bulkWorkFactoryMock.apply( firstBulkWorks ) ).andReturn( (ElasticsearchWork) bulkWork1 );
		replayAll();
		for ( BulkableElasticsearchWork<?> work : firstBulkWorks ) {
			bulker.add( work );
		}
		verifyAll();
		assertThat( bulkWork1FutureCapture.getValue() ).isSuccessful( bulkWork1 );

		resetAll();
		replayAll();
		bulker.add( additionalWork1 );
		bulker.add( additionalWork2 );
		verifyAll();

		resetAll();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork2FutureCapture ) ) ).andReturn( bulkWork2ResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWork2ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( additionalWork1, 0 );
		bulkResultExtractionStepMock.add( additionalWork2, 1 );
		replayAll();
		bulker.flushBulked();
		verifyAll();
		assertThat( bulkWork2FutureCapture.getValue() ).isPending();

		resetAll();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( additionalWork1, additionalWork2 ) ) ).andReturn( (ElasticsearchWork) bulkWork2 );
		replayAll();
		bulker.flushBulk();
		verifyAll();
		assertThat( bulkWork2FutureCapture.getValue() ).isSuccessful( bulkWork2 );
	}

	private <T> ElasticsearchWork<T> work(int index) {
		ElasticsearchWork<T> mock = createStrictMock( "work" + index, ElasticsearchWork.class );
		return mock;
	}

	private <T> BulkableElasticsearchWork<T> bulkableWork(int index) {
		BulkableElasticsearchWork<T> mock = createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
		return mock;
	}
}
