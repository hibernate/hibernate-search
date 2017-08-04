/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hibernate.search.test.util.FutureAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.easymock.EasyMock;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.elasticsearch.work.impl.BulkResult;
import org.hibernate.search.elasticsearch.work.impl.BulkResultItemExtractor;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchWorkSequenceBuilderTest {

	private final List<Object> mocks = new ArrayList<>();

	private ElasticsearchWorkExecutor executorMock;
	private FlushableElasticsearchWorkExecutionContext contextMock;
	private Supplier<FlushableElasticsearchWorkExecutionContext> contextSupplierMock;
	private ContextualErrorHandler errorHandlerMock;
	private Supplier<ContextualErrorHandler> errorHandlerSupplierMock;

	@Before
	@SuppressWarnings("unchecked")
	public void initMocks() {
		executorMock = EasyMock.createStrictMock( ElasticsearchWorkExecutor.class );
		contextMock = EasyMock.createStrictMock( FlushableElasticsearchWorkExecutionContext.class );
		contextSupplierMock = EasyMock.createStrictMock( Supplier.class );
		errorHandlerMock = EasyMock.createStrictMock( ContextualErrorHandler.class );
		errorHandlerSupplierMock = EasyMock.createStrictMock( Supplier.class );
		mocks.addAll( Arrays.asList( executorMock, contextMock, contextSupplierMock, errorHandlerMock, errorHandlerSupplierMock ) );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void simple() {
		ElasticsearchWork<Void> work1 = work( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture );
		verify();

		reset();
		replay();
		builder.addNonBulkExecution( work1 );
		verify();

		reset();
		replay();
		builder.addNonBulkExecution( work2 );
		verify();

		reset();
		replay();
		CompletableFuture<Void> futureSequence = builder.build();
		verify();
		assertThat( futureSequence ).isPending();

		reset();
		expect( executorMock.submit( work1, contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replay();
		previousFuture.complete( null );
		verify();
		assertThat( futureSequence ).isPending();

		reset();
		expect( executorMock.submit( work2, contextMock ) ).andReturn( (CompletableFuture) work2Future );
		replay();
		work1Future.complete( null );
		verify();
		assertThat( futureSequence ).isPending();

		reset();
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		work2Future.complete( null );
		verify();
		assertThat( futureSequence ).isPending();

		reset();
		replay();
		flushFuture.complete( null );
		verify();
		assertThat( futureSequence ).isSuccessful( (Void) null );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void bulk() {
		ElasticsearchWork<Void> work1 = work( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<Void> work4 = work( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		BulkResult bulkResultMock = EasyMock.createStrictMock( BulkResult.class );
		mocks.add( bulkResultMock );
		BulkResultItemExtractor bulkItemResultExtractorMock = EasyMock.createStrictMock( BulkResultItemExtractor.class );
		mocks.add( bulkItemResultExtractorMock );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> work4Future = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture );
		verify();

		reset();
		replay();
		builder.addNonBulkExecution( work1 );
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.startBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work2, 0 );
		extractionStep.add( work3, 1 );
		builder.addNonBulkExecution( work4 );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();

		reset();
		replay();
		CompletableFuture<Void> sequenceFuture = builder.build();
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( executorMock.submit( work1, contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replay();
		previousFuture.complete( null );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		replay();
		work1Future.complete( null );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( executorMock.submit( bulkWork, contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replay();
		bulkWorkFuture.complete( bulkWork );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkItemResultExtractorMock );
		expect( bulkItemResultExtractorMock.extract( work2, 0 ) ).andReturn( work2Future );
		expect( bulkItemResultExtractorMock.extract( work3, 1 ) ).andReturn( work3Future );
		replay();
		bulkResultFuture.complete( bulkResultMock );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( sequenceFuture ).isPending();

		reset();
		replay();
		work3Future.complete( null );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( executorMock.submit( work4, contextMock ) ).andReturn( (CompletableFuture) work4Future );
		replay();
		work2Future.complete( null );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		work4Future.complete( null );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		replay();
		flushFuture.complete( null );
		verify();
		assertThat( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void newSequenceOnReset() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		CompletableFuture<?> previousFuture1 = new CompletableFuture<>();
		CompletableFuture<?> previousFuture2 = new CompletableFuture<>();
		// We'll never complete this future, so as to check that work 2 is executed in a different sequence
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2FlushFuture = new CompletableFuture<>();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture1 );
		verify();

		reset();
		replay();
		builder.addNonBulkExecution( work1 );
		verify();

		reset();
		replay();
		CompletableFuture<Void> sequence1Future = builder.build();
		verify();
		assertThat( sequence1Future ).isPending();

		reset();
		expect( executorMock.submit( work1, contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replay();
		previousFuture1.complete( null );
		verify();
		assertThat( sequence1Future ).isPending();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture2 );
		verify();
		assertThat( sequence1Future ).isPending();

		reset();
		replay();
		builder.addNonBulkExecution( work2 );
		verify();
		assertThat( sequence1Future ).isPending();

		reset();
		replay();
		CompletableFuture<Void> sequence2Future = builder.build();
		verify();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		reset();
		expect( executorMock.submit( work2, contextMock ) ).andReturn( (CompletableFuture) work2Future );
		replay();
		previousFuture2.complete( null );
		verify();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		reset();
		expect( contextMock.flush() ).andReturn( sequence2FlushFuture );
		replay();
		work2Future.complete( null );
		verify();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		reset();
		replay();
		sequence2FlushFuture.complete( null );
		verify();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isSuccessful( (Void) null );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void error_work() {
		ElasticsearchWork<Void> work1 = work( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		MyException exception = new MyException();

		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		expect( executorMock.submit( work1, contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		builder.init( previousFuture );
		builder.addNonBulkExecution( work1 );
		builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsFailed( work1, exception );
		errorHandlerMock.markAsSkipped( work2 );
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		work1Future.completeExceptionally( exception );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.handle();
		replay();
		flushFuture.complete( null );
		verify();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_work() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		MyException exception = new MyException();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture );
		verify();

		reset();
		replay();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.startBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work1, 0 );
		extractionStep.add( work2, 1 );
		extractionStep.add( work3, 2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsSkipped( work1 );
		errorHandlerMock.markAsSkipped( work2 );
		errorHandlerMock.markAsSkipped( work3 );
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		bulkWorkFuture.completeExceptionally( exception );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isFailed( exception );
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.addThrowable( exception );
		errorHandlerMock.handle();
		replay();
		flushFuture.complete( null );
		verify();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void error_bulk_result() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkWorkResultMock = EasyMock.createStrictMock( BulkResult.class );
		mocks.add( bulkWorkResultMock );
		BulkResultItemExtractor bulkResultItemExtractorMock = EasyMock.createStrictMock( BulkResultItemExtractor.class );
		mocks.add( bulkResultItemExtractorMock );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		MyException exception = new MyException();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture );
		verify();


		reset();
		replay();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.startBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work1, 0 );
		extractionStep.add( work2, 1 );
		extractionStep.add( work3, 2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( executorMock.submit( bulkWork, contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replay();
		bulkWorkFuture.complete( bulkWork );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsSkipped( work1 );
		errorHandlerMock.markAsSkipped( work2 );
		errorHandlerMock.markAsSkipped( work3 );
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		bulkResultFuture.completeExceptionally( exception );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isFailed( exception );
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.addThrowable( exception );
		errorHandlerMock.handle();
		replay();
		flushFuture.complete( null );
		verify();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void error_bulk_resultExtraction_singleFailure() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<Void> work4 = bulkableWork( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		BulkResult bulkResultMock = EasyMock.createStrictMock( BulkResult.class );
		mocks.add( bulkResultMock );
		BulkResultItemExtractor bulkResultItemExtractorMock = EasyMock.createStrictMock( BulkResultItemExtractor.class );
		mocks.add( bulkResultItemExtractorMock );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		MyRuntimeException exception = new MyRuntimeException();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture );
		verify();

		reset();
		replay();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.startBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work1, 0 );
		extractionStep.add( work2, 1 );
		extractionStep.add( work3, 2 );
		builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( executorMock.submit( bulkWork, contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replay();
		bulkWorkFuture.complete( bulkWork );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andThrow( exception );
		expect( bulkResultItemExtractorMock.extract( work3, 2 ) ).andReturn( work3Future );
		errorHandlerMock.markAsFailed( work2, exception );
		replay();
		bulkResultFuture.complete( bulkResultMock );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( sequenceFuture ).isPending();

		reset();
		replay();
		work1Future.complete( null );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsSkipped( work4 );
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		work3Future.complete( null );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.handle();
		replay();
		flushFuture.complete( null );
		verify();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void error_bulk_resultExtraction_multipleFailures() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = EasyMock.createStrictMock( BulkResult.class );
		mocks.add( bulkResultMock );
		BulkResultItemExtractor bulkItemResultExtractorMock = EasyMock.createStrictMock( BulkResultItemExtractor.class );
		mocks.add( bulkItemResultExtractorMock );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		MyRuntimeException exception1 = new MyRuntimeException();
		MyRuntimeException exception2 = new MyRuntimeException();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture );
		verify();


		reset();
		replay();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.startBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work1, 0 );
		extractionStep.add( work2, 1 );
		builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( executorMock.submit( bulkWork, contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replay();
		bulkWorkFuture.complete( bulkWork );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkItemResultExtractorMock );
		expect( bulkItemResultExtractorMock.extract( work1, 0 ) ).andThrow( exception1 );
		expect( bulkItemResultExtractorMock.extract( work2, 1 ) ).andThrow( exception2 );
		errorHandlerMock.markAsFailed( work1, exception1 );
		errorHandlerMock.markAsFailed( work2, exception2 );
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		bulkResultFuture.complete( bulkResultMock );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.handle();
		replay();
		flushFuture.complete( null );
		verify();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}


	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void error_bulk_resultExtraction_future_singleFailure() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<Void> work4 = bulkableWork( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		BulkResult bulkResultMock = EasyMock.createStrictMock( BulkResult.class );
		mocks.add( bulkResultMock );
		BulkResultItemExtractor bulkResultItemExtractorMock = EasyMock.createStrictMock( BulkResultItemExtractor.class );
		mocks.add( bulkResultItemExtractorMock );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		MyException exception = new MyException();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture );
		verify();

		reset();
		replay();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.startBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work1, 0 );
		extractionStep.add( work2, 1 );
		extractionStep.add( work3, 2 );
		builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( executorMock.submit( bulkWork, contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replay();
		bulkWorkFuture.complete( bulkWork );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andReturn( work2Future );
		expect( bulkResultItemExtractorMock.extract( work3, 2 ) ).andReturn( work3Future );
		replay();
		bulkResultFuture.complete( bulkResultMock );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( sequenceFuture ).isPending();

		reset();
		replay();
		work1Future.complete( null );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsFailed( work2, exception );
		replay();
		work2Future.completeExceptionally( exception );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsSkipped( work4 );
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		work3Future.complete( null );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.handle();
		replay();
		flushFuture.complete( null );
		verify();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void error_bulk_resultExtraction_future_multipleFailures() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = EasyMock.createStrictMock( BulkResult.class );
		mocks.add( bulkResultMock );
		BulkResultItemExtractor bulkResultItemExtractorMock = EasyMock.createStrictMock( BulkResultItemExtractor.class );
		mocks.add( bulkResultItemExtractorMock );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		MyException exception1 = new MyException();
		MyException exception2 = new MyException();

		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		verify();

		reset();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replay();
		builder.init( previousFuture );
		verify();

		reset();
		replay();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.startBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work1, 0 );
		extractionStep.add( work2, 1 );
		builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( executorMock.submit( bulkWork, contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replay();
		bulkWorkFuture.complete( bulkWork );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		reset();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andReturn( work2Future );
		replay();
		bulkResultFuture.complete( bulkResultMock );
		verify();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsFailed( work2, exception2 );
		replay();
		work2Future.completeExceptionally( exception2 );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsFailed( work1, exception1 );
		errorHandlerMock.markAsSkipped( work3 );
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		work1Future.completeExceptionally( exception1 );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.handle();
		replay();
		flushFuture.complete( null );
		verify();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void error_handler() {
		ElasticsearchWork<Void> work1 = work( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> flushFuture = new CompletableFuture<>();

		MyException exception = new MyException();
		IllegalStateException handlerException = new IllegalStateException();

		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		expect( executorMock.submit( work1, contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replay();
		ElasticsearchWorkSequenceBuilder builder = new DefaultElasticsearchWorkSequenceBuilder(
				executorMock, contextSupplierMock, errorHandlerSupplierMock );
		builder.init( previousFuture );
		builder.addNonBulkExecution( work1 );
		builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.markAsFailed( work1, exception );
		errorHandlerMock.markAsSkipped( work2 );
		expect( contextMock.flush() ).andReturn( flushFuture );
		replay();
		work1Future.completeExceptionally( exception );
		verify();
		assertThat( sequenceFuture ).isPending();

		reset();
		errorHandlerMock.handle();
		expectLastCall().andThrow( handlerException );
		replay();
		flushFuture.complete( null );
		verify();
		// Errors MUST be propagated if they originated from the handler (critical failure)
		assertThat( sequenceFuture ).isFailed( handlerException );
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

	private BulkableElasticsearchWork<Void> bulkableWork(int index) {
		@SuppressWarnings("unchecked")
		BulkableElasticsearchWork<Void> mock = EasyMock.createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
		mocks.add( mock );
		return mock;
	}

	private static class MyException extends Exception {
	}

	private static class MyRuntimeException extends RuntimeException {
	}
}
