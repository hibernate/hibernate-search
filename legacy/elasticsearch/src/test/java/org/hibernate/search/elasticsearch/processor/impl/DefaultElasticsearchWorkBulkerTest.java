/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hibernate.search.test.util.FutureAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.elasticsearch.work.impl.BulkResult;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchWorkBulkerTest {

	private static final int DEFAULT_MIN_BULK_SIZE = 1;

	private static final int DEFAULT_MAX_BULK_SIZE = 10;

	private final List<Object> mocks = new ArrayList<>();

	private ElasticsearchWorkSequenceBuilder sequenceBuilderMock;
	private BulkResultExtractionStep bulkResultExtractionStepMock;
	private Function<List<BulkableElasticsearchWork<?>>, ElasticsearchWork<BulkResult>> bulkWorkFactoryMock;

	@Before
	@SuppressWarnings("unchecked")
	public void initMocks() {
		sequenceBuilderMock = EasyMock.createStrictMock( ElasticsearchWorkSequenceBuilder.class );
		bulkResultExtractionStepMock = EasyMock.createStrictMock( BulkResultExtractionStep.class );
		bulkWorkFactoryMock = EasyMock.createStrictMock( Function.class );
		mocks.addAll( Arrays.asList( sequenceBuilderMock, bulkWorkFactoryMock ) );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void simple() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		ElasticsearchWork<BulkResult> bulkWork = work( 3 );

		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWorkFutureCapture = new Capture<>();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		replay();
		DefaultElasticsearchWorkBulker bulker =
				new DefaultElasticsearchWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						DEFAULT_MIN_BULK_SIZE, DEFAULT_MAX_BULK_SIZE );
		verify();

		reset();
		replay();
		bulker.add( work1 );
		verify();

		reset();
		replay();
		bulker.add( work2 );
		verify();

		reset();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWorkFutureCapture ) ) ).andReturn( bulkWorkResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work1, 0 );
		bulkResultExtractionStepMock.add( work2, 1 );
		replay();
		bulker.flushBulked();
		verify();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		reset();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2 ) ) ).andReturn( (ElasticsearchWork) bulkWork );
		replay();
		bulker.flushBulk();
		verify();
		assertThat( bulkWorkFutureCapture.getValue() ).isSuccessful( bulkWork );
	}

	@Test
	public void noBulkIfBelowThreshold() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );

		replay();
		DefaultElasticsearchWorkBulker bulker =
				new DefaultElasticsearchWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						2 /* Mandate minimum 2 works per bulk */,
						DEFAULT_MAX_BULK_SIZE );
		verify();

		reset();
		replay();
		bulker.add( work1 );
		verify();

		reset();
		sequenceBuilderMock.addNonBulkExecution( work1 );
		replay();
		bulker.flushBulked();
		verify();

		reset();
		replay();
		bulker.flushBulk();
		verify();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void alwaysBulkIfAboveThreshold() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		ElasticsearchWork<BulkResult> bulkWork = work( 2 );

		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWorkFutureCapture = new Capture<>();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		replay();
		DefaultElasticsearchWorkBulker bulker =
				new DefaultElasticsearchWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						1 /* No threshold, even 1 work per bulk is okay */,
						DEFAULT_MAX_BULK_SIZE );
		verify();

		reset();
		replay();
		bulker.add( work1 );
		verify();


		reset();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWorkFutureCapture ) ) ).andReturn( bulkWorkResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work1, 0 );
		replay();
		bulker.flushBulked();
		verify();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		reset();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1 ) ) ).andReturn( (ElasticsearchWork) bulkWork );
		replay();
		bulker.flushBulk();
		verify();
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

		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWorkFutureCapture = new Capture<>();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		replay();
		DefaultElasticsearchWorkBulker bulker =
				new DefaultElasticsearchWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						DEFAULT_MIN_BULK_SIZE, DEFAULT_MAX_BULK_SIZE );
		verify();

		reset();
		replay();
		bulker.add( work1 );
		bulker.add( work2 );
		verify();

		reset();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWorkFutureCapture ) ) ).andReturn( bulkWorkResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work1, 0 );
		bulkResultExtractionStepMock.add( work2, 1 );
		replay();
		bulker.flushBulked();
		verify();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		reset();
		replay();
		bulker.add( work3 );
		bulker.add( work4 );
		verify();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		reset();
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWorkResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work3, 2 );
		bulkResultExtractionStepMock.add( work4, 3 );
		replay();
		bulker.flushBulked();
		verify();
		assertThat( bulkWorkFutureCapture.getValue() ).isPending();

		reset();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2, work3, work4 ) ) ).andReturn( (ElasticsearchWork) bulkWork );
		replay();
		bulker.flushBulk();
		verify();
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
		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWork1FutureCapture = new Capture<>();
		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWork2FutureCapture = new Capture<>();

		replay();
		DefaultElasticsearchWorkBulker bulker =
				new DefaultElasticsearchWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						DEFAULT_MIN_BULK_SIZE, DEFAULT_MAX_BULK_SIZE );
		verify();

		reset();
		replay();
		bulker.add( work1 );
		bulker.add( work2 );
		verify();

		reset();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork1FutureCapture ) ) ).andReturn( bulkWork1ResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWork1ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work1, 0 );
		bulkResultExtractionStepMock.add( work2, 1 );
		replay();
		bulker.flushBulked();
		verify();
		assertThat( bulkWork1FutureCapture.getValue() ).isPending();

		reset();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2 ) ) ).andReturn( (ElasticsearchWork) bulkWork1 );
		replay();
		bulker.flushBulk();
		verify();
		assertThat( bulkWork1FutureCapture.getValue() ).isSuccessful( bulkWork1 );

		reset();
		replay();
		bulker.add( work3 );
		bulker.add( work4 );
		verify();

		reset();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork2FutureCapture ) ) ).andReturn( bulkWork2ResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWork2ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( work3, 0 );
		bulkResultExtractionStepMock.add( work4, 1 );
		replay();
		bulker.flushBulked();
		verify();
		assertThat( bulkWork2FutureCapture.getValue() ).isPending();

		reset();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( work3, work4 ) ) ).andReturn( (ElasticsearchWork) bulkWork2 );
		replay();
		bulker.flushBulk();
		verify();
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
		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWork1FutureCapture = new Capture<>();
		Capture<CompletableFuture<ElasticsearchWork<BulkResult>>> bulkWork2FutureCapture = new Capture<>();

		replay();
		DefaultElasticsearchWorkBulker bulker =
				new DefaultElasticsearchWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock,
						DEFAULT_MIN_BULK_SIZE, DEFAULT_MAX_BULK_SIZE );
		verify();

		reset();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork1FutureCapture ) ) ).andReturn( bulkWork1ResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWork1ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		for ( int i = 0 ; i < DEFAULT_MAX_BULK_SIZE ; ++i ) {
			BulkableElasticsearchWork<?> work = firstBulkWorks.get( i );
			bulkResultExtractionStepMock.add( work, i );
		}
		expect( bulkWorkFactoryMock.apply( firstBulkWorks ) ).andReturn( (ElasticsearchWork) bulkWork1 );
		replay();
		for ( BulkableElasticsearchWork<?> work : firstBulkWorks ) {
			bulker.add( work );
		}
		verify();
		assertThat( bulkWork1FutureCapture.getValue() ).isSuccessful( bulkWork1 );

		reset();
		replay();
		bulker.add( additionalWork1 );
		bulker.add( additionalWork2 );
		verify();

		reset();
		expect( sequenceBuilderMock.addBulkExecution( capture( bulkWork2FutureCapture ) ) ).andReturn( bulkWork2ResultFuture );
		expect( sequenceBuilderMock.startBulkResultExtraction( bulkWork2ResultFuture ) ).andReturn( bulkResultExtractionStepMock );
		bulkResultExtractionStepMock.add( additionalWork1, 0 );
		bulkResultExtractionStepMock.add( additionalWork2, 1 );
		replay();
		bulker.flushBulked();
		verify();
		assertThat( bulkWork2FutureCapture.getValue() ).isPending();

		reset();
		expect( bulkWorkFactoryMock.apply( Arrays.asList( additionalWork1, additionalWork2 ) ) ).andReturn( (ElasticsearchWork) bulkWork2 );
		replay();
		bulker.flushBulk();
		verify();
		assertThat( bulkWork2FutureCapture.getValue() ).isSuccessful( bulkWork2 );
	}

	private void reset() {
		EasyMock.reset( mocks.toArray() );
	}

	private void replay() {
		EasyMock.replay( mocks.toArray() );
	}

	private void verify() {
		EasyMock.verify( mocks.toArray() );
	}

	private <T> ElasticsearchWork<T> work(int index) {
		@SuppressWarnings("unchecked")
		ElasticsearchWork<T> mock = EasyMock.createStrictMock( "work" + index, ElasticsearchWork.class );
		mocks.add( mock );
		return mock;
	}

	private <T> BulkableElasticsearchWork<T> bulkableWork(int index) {
		@SuppressWarnings("unchecked")
		BulkableElasticsearchWork<T> mock = EasyMock.createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
		mocks.add( mock );
		return mock;
	}
}
