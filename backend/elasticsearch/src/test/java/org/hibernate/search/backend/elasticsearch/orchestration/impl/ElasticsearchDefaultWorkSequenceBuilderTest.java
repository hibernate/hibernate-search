/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hibernate.search.util.impl.test.ExceptionMatcherBuilder.isException;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResultItemExtractor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.util.common.SearchException;

import org.junit.Before;
import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.HamcrestCondition;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;


@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class ElasticsearchDefaultWorkSequenceBuilderTest extends EasyMockSupport {

	private ElasticsearchRefreshableWorkExecutionContext contextMock;
	private Supplier<ElasticsearchRefreshableWorkExecutionContext> contextSupplierMock;
	private FailureHandler failureHandlerMock;

	@Before
	public void initMocks() {
		contextMock = createStrictMock( ElasticsearchRefreshableWorkExecutionContext.class );
		contextSupplierMock = createStrictMock( Supplier.class );
		failureHandlerMock = createStrictMock( FailureHandler.class );
	}

	@Test
	public void simple() {
		ElasticsearchWork<Object> work1 = work( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );

		Object work1Result = new Object();
		Object work2Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();

		resetAll();
		replayAll();
		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();

		resetAll();
		replayAll();
		CompletableFuture<Void> sequenceFuture = builder.build();
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		previousFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work2.execute( contextMock ) ).andReturn( (CompletableFuture) work2Future );
		replayAll();
		work1Future.complete( work1Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	public void bulk() {
		ElasticsearchWork<Object> work1 = work( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		ElasticsearchWork<Object> work4 = work( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work2Result = new Object();
		Object work3Result = new Object();
		Object work4Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkItemResultExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();
		CompletableFuture<Object> work4Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Object> work4FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 0 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 1 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();

		resetAll();
		replayAll();
		CompletableFuture<Void> sequenceFuture = builder.build();
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work1Future.complete( work1Result );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for the refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkItemResultExtractorMock );
		expect( bulkItemResultExtractorMock.extract( work2, 0 ) ).andReturn( work2Future );
		expect( bulkItemResultExtractorMock.extract( work3, 1 ) ).andReturn( work3Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work3Future.complete( work3Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work4.execute( contextMock ) ).andReturn( (CompletableFuture) work4Future );
		replayAll();
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work4Future.complete( work4Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThat( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		assertThat( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	public void newSequenceOnReset() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture1 = new CompletableFuture<>();
		CompletableFuture<?> previousFuture2 = new CompletableFuture<>();
		// We'll never complete this future, so as to check that work 2 is executed in a different sequence
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2refreshFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture1 );
		verifyAll();

		resetAll();
		replayAll();
		builder.addNonBulkExecution( work1 );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<Void> sequence1Future = builder.build();
		verifyAll();
		assertThat( sequence1Future ).isPending();

		resetAll();
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		previousFuture1.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture2 );
		verifyAll();
		assertThat( sequence1Future ).isPending();

		resetAll();
		replayAll();
		builder.addNonBulkExecution( work2 );
		verifyAll();
		assertThat( sequence1Future ).isPending();

		resetAll();
		replayAll();
		CompletableFuture<Void> sequence2Future = builder.build();
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		resetAll();
		expect( work2.execute( contextMock ) ).andReturn( (CompletableFuture) work2Future );
		replayAll();
		previousFuture2.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		resetAll();
		expect( contextMock.executePendingRefreshes() ).andReturn( sequence2refreshFuture );
		replayAll();
		work2Future.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		resetAll();
		replayAll();
		sequence2refreshFuture.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isSuccessful( (Void) null );
	}

	@Test
	public void error_work() {
		ElasticsearchWork<Object> work0 = work( 0 );
		ElasticsearchWork<Void> work1 = work( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		Object work0Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work0FutureFromSequenceBuilder;
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;

		MyException exception = new MyException();

		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( work0.execute( contextMock ) ).andReturn( (CompletableFuture) work0Future );
		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		builder.init( previousFuture );
		work0FutureFromSequenceBuilder = builder.addNonBulkExecution( work0 );
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( work0FutureFromSequenceBuilder ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		work0Future.complete( work0Result );
		verifyAll();
		assertThat( work0FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.getInfo() ).andReturn( "work1" );
		expect( work2.getInfo() ).andReturn( "work2" );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work1Future.completeExceptionally( exception );
		verifyAll();
		assertThat( work0FutureFromSequenceBuilder ).isPending();
		// Errors must be propagated to the individual work futures ASAP
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception );
		// Subsequent works that haven't been executed must get a specific exception
		assertThat( work2FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation was skipped due to the failure of a previous work in the same workset" )
						.causedBy( exception ).build()
		);
		// But the sequence future must wait for the refresh to happen
		assertThat( sequenceFuture ).isPending();

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Works that happened before the error must be considered as successful if the refresh is successful
		assertThat( work0FutureFromSequenceBuilder ).isSuccessful( work0Result );
		// Errors MUST be propagated to the sequence future
		assertThat( sequenceFuture ).isFailed(
				isException( SearchException.class )
						.causedBy( exception )
						.build()
		);

		IndexFailureContext failureContext = failureContextCapture.getValue();
		Assertions.assertThat( failureContext.getThrowable() ).isSameAs( exception );
		Assertions.assertThat( failureContext.getFailingOperation() )
				.isEqualTo( "work1" );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly( "work1", "work2" );
	}

	@Test
	public void error_bulk_work() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<Void> work4 = work( 4 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Void> work3FutureFromSequenceBuilder;

		MyException exception = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 2 );
		builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.getInfo() ).andReturn( "work1" );
		expect( work2.getInfo() ).andReturn( "work2" );
		expect( work3.getInfo() ).andReturn( "work3" );
		expect( work4.getInfo() ).andReturn( "work4" );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		bulkWorkFuture.completeExceptionally( exception );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isFailed( exception );
		assertThat( work1FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		);
		assertThat( work2FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		);
		assertThat( work3FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		);
		assertThat( sequenceFuture ).isPending();

		Capture<IndexFailureContext> failureContext1Capture = Capture.newInstance();
		Capture<IndexFailureContext> failureContext2Capture = Capture.newInstance();
		Capture<IndexFailureContext> failureContext3Capture = Capture.newInstance();
		resetAll();
		failureHandlerMock.handle( capture( failureContext1Capture ) );
		failureHandlerMock.handle( capture( failureContext2Capture ) );
		failureHandlerMock.handle( capture( failureContext3Capture ) );
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST be propagated to the sequence future
		assertThat( sequenceFuture ).isFailed(
				isException( SearchException.class )
						.causedBy( exception )
						.build()
		);

		IndexFailureContext failureContext1 = failureContext1Capture.getValue();
		Assertions.assertThat( failureContext1.getThrowable() ).satisfies( new HamcrestCondition<>(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		) );
		Assertions.assertThat( failureContext1.getFailingOperation() )
				.isEqualTo( "work1" );
		Assertions.<Object>assertThat( failureContext1.getUncommittedOperations() )
				// Skipped works are blamed on the first failed work
				.containsExactly( "work1", "work4" );

		IndexFailureContext failureContext2 = failureContext2Capture.getValue();
		Assertions.assertThat( failureContext2.getThrowable() ).satisfies( new HamcrestCondition<>(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		) );
		Assertions.assertThat( failureContext2.getFailingOperation() )
				.isEqualTo( "work2" );
		Assertions.<Object>assertThat( failureContext2.getUncommittedOperations() )
				.containsExactly( "work2" );

		IndexFailureContext failureContext3 = failureContext3Capture.getValue();
		Assertions.assertThat( failureContext3.getThrowable() ).satisfies( new HamcrestCondition<>(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		) );
		Assertions.assertThat( failureContext3.getFailingOperation() )
				.isEqualTo( "work3" );
		Assertions.<Object>assertThat( failureContext3.getUncommittedOperations() )
				.containsExactly( "work3" );
	}

	@Test
	public void error_bulk_result() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );
		ElasticsearchWork<Void> work4 = work( 5 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Void> work3FutureFromSequenceBuilder;

		MyException exception = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 2 );
		builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.getInfo() ).andReturn( "work1" );
		expect( work2.getInfo() ).andReturn( "work2" );
		expect( work3.getInfo() ).andReturn( "work3" );
		expect( work4.getInfo() ).andReturn( "work4" );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		bulkResultFuture.completeExceptionally( exception );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isFailed( exception );
		assertThat( work1FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		);
		assertThat( work2FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		);
		assertThat( work3FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		);
		assertThat( sequenceFuture ).isPending();

		Capture<IndexFailureContext> failureContext1Capture = Capture.newInstance();
		Capture<IndexFailureContext> failureContext2Capture = Capture.newInstance();
		Capture<IndexFailureContext> failureContext3Capture = Capture.newInstance();
		resetAll();
		failureHandlerMock.handle( capture( failureContext1Capture ) );
		failureHandlerMock.handle( capture( failureContext2Capture ) );
		failureHandlerMock.handle( capture( failureContext3Capture ) );
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST be propagated to the sequence future
		assertThat( sequenceFuture ).isFailed(
				isException( SearchException.class )
						.causedBy( exception )
						.build()
		);

		IndexFailureContext failureContext1 = failureContext1Capture.getValue();
		Assertions.assertThat( failureContext1.getThrowable() ).satisfies( new HamcrestCondition<>(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		) );
		Assertions.assertThat( failureContext1.getFailingOperation() )
				.isEqualTo( "work1" );
		Assertions.<Object>assertThat( failureContext1.getUncommittedOperations() )
				// Skipped works are blamed on the first failed work
				.containsExactly( "work1", "work4" );

		IndexFailureContext failureContext2 = failureContext2Capture.getValue();
		Assertions.assertThat( failureContext2.getThrowable() ).satisfies( new HamcrestCondition<>(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		) );
		Assertions.assertThat( failureContext2.getFailingOperation() )
				.isEqualTo( "work2" );
		Assertions.<Object>assertThat( failureContext2.getUncommittedOperations() )
				.containsExactly( "work2" );

		IndexFailureContext failureContext3 = failureContext3Capture.getValue();
		Assertions.assertThat( failureContext3.getThrowable() ).satisfies( new HamcrestCondition<>(
				isException( SearchException.class )
						.withMessage( "operation failed due to the failure of the call to the bulk REST API" )
						.causedBy( exception ).build()
		) );
		Assertions.assertThat( failureContext3.getFailingOperation() )
				.isEqualTo( "work3" );
		Assertions.<Object>assertThat( failureContext3.getUncommittedOperations() )
				.containsExactly( "work3" );
	}

	@Test
	public void error_bulk_resultExtraction_singleFailure() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<Void> work4 = bulkableWork( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work3Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkResultItemExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Void> work4FutureFromSequenceBuilder;

		MyRuntimeException exception = new MyRuntimeException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 2 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andThrow( exception );
		expect( bulkResultItemExtractorMock.extract( work3, 2 ) ).andReturn( work3Future );
		expect( work2.getInfo() ).andReturn( "work2" );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work1Future.complete( work1Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work4.getInfo() ).andReturn( "work4" );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work3Future.complete( work3Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work4FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation was skipped due to the failure of a previous work in the same workset" )
						.causedBy( exception ).build()
		);
		assertThat( sequenceFuture ).isPending();

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		// Errors MUST be propagated to the sequence future
		assertThat( sequenceFuture ).isFailed( exception );

		IndexFailureContext failureContext = failureContextCapture.getValue();
		Assertions.assertThat( failureContext.getThrowable() ).isSameAs( exception );
		Assertions.assertThat( failureContext.getFailingOperation() )
				.isEqualTo( "work2" );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				// Skipped works are blamed on the first failed work
				.containsExactly( "work2", "work4" );
	}

	@Test
	public void error_bulk_resultExtraction_multipleFailures() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkItemResultExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;

		MyRuntimeException exception1 = new MyRuntimeException();
		MyRuntimeException exception2 = new MyRuntimeException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkItemResultExtractorMock );
		expect( bulkItemResultExtractorMock.extract( work1, 0 ) ).andThrow( exception1 );
		expect( bulkItemResultExtractorMock.extract( work2, 1 ) ).andThrow( exception2 );
		expect( work1.getInfo() ).andReturn( "work1" );
		expect( work2.getInfo() ).andReturn( "work2" );
		expect( work3.getInfo() ).andReturn( "work3" );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception1 );
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception2 );
		assertThat( work3FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation was skipped due to the failure of a previous work in the same workset" )
						.causedBy( exception1 ).build()
		);
		assertThat( sequenceFuture ).isPending();

		Capture<IndexFailureContext> failureContext1Capture = Capture.newInstance();
		Capture<IndexFailureContext> failureContext2Capture = Capture.newInstance();
		resetAll();
		failureHandlerMock.handle( capture( failureContext1Capture ) );
		failureHandlerMock.handle( capture( failureContext2Capture ) );
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST be propagated to the sequence future
		assertThat( sequenceFuture ).isFailed( exception1 );

		IndexFailureContext failureContext1 = failureContext1Capture.getValue();
		Assertions.assertThat( failureContext1.getThrowable() ).isSameAs( exception1 );
		Assertions.assertThat( failureContext1.getFailingOperation() )
				.isEqualTo( "work1" );
		Assertions.<Object>assertThat( failureContext1.getUncommittedOperations() )
				// Skipped works are blamed on the first failed work
				.containsExactly( "work1", "work3" );

		IndexFailureContext failureContext2 = failureContext2Capture.getValue();
		Assertions.assertThat( failureContext2.getThrowable() ).isSameAs( exception2 );
		Assertions.assertThat( failureContext2.getFailingOperation() )
				.isEqualTo( "work2" );
		Assertions.<Object>assertThat( failureContext2.getUncommittedOperations() )
				.containsExactly( "work2" );
	}


	@Test
	public void error_bulk_resultExtraction_future_singleFailure() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<Void> work4 = bulkableWork( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work3Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkResultItemExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Void> work4FutureFromSequenceBuilder;

		MyException exception = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 2 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andReturn( work2Future );
		expect( bulkResultItemExtractorMock.extract( work3, 2 ) ).andReturn( work3Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work1Future.complete( work1Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still waiting for the refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work2.getInfo() ).andReturn( "work2" );
		replayAll();
		work2Future.completeExceptionally( exception );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work4.getInfo() ).andReturn( "work4" );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work3Future.complete( work3Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending(); // Still waiting for the refresh
		assertThat( work4FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation was skipped due to the failure of a previous work in the same workset" )
						.causedBy( exception ).build()
		);
		assertThat( sequenceFuture ).isPending();

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		// Errors MUST be propagated to the sequence future
		assertThat( sequenceFuture ).isFailed(
				isException( SearchException.class )
						.causedBy( exception )
						.build()
		);

		IndexFailureContext failureContext = failureContextCapture.getValue();
		Assertions.assertThat( failureContext.getThrowable() ).isSameAs( exception );
		Assertions.assertThat( failureContext.getFailingOperation() )
				.isEqualTo( "work2" );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				// Skipped works are blamed on the first failed work
				.containsExactly( "work2", "work4" );
	}

	@Test
	public void error_bulk_resultExtraction_future_multipleFailures() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkResultItemExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;

		MyException exception1 = new MyException();
		MyException exception2 = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andReturn( work2Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work2.getInfo() ).andReturn( "work2" );
		replayAll();
		work2Future.completeExceptionally( exception2 );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception2 );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.getInfo() ).andReturn( "work1" );
		expect( work3.getInfo() ).andReturn( "work3" );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work1Future.completeExceptionally( exception1 );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception1 );
		assertThat( work3FutureFromSequenceBuilder ).isFailed(
				isException( SearchException.class )
						.withMessage( "operation was skipped due to the failure of a previous work in the same workset" )
						.causedBy( exception1 ).build()
		);
		assertThat( sequenceFuture ).isPending();

		Capture<IndexFailureContext> failureContext2Capture = Capture.newInstance();
		Capture<IndexFailureContext> failureContext1Capture = Capture.newInstance();
		resetAll();
		failureHandlerMock.handle( capture( failureContext2Capture ) );
		failureHandlerMock.handle( capture( failureContext1Capture ) );
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST be propagated to the sequence future
		assertThat( sequenceFuture ).isFailed(
				isException( SearchException.class )
						.causedBy( exception2 )
						.withSuppressed( exception1 )
						.build()
		);

		IndexFailureContext failureContext2 = failureContext2Capture.getValue();
		Assertions.assertThat( failureContext2.getThrowable() ).isSameAs( exception2 );
		Assertions.assertThat( failureContext2.getFailingOperation() )
				.isEqualTo( "work2" );
		Assertions.<Object>assertThat( failureContext2.getUncommittedOperations() )
				// Skipped works are blamed on the first failed work (in execution order)
				.containsExactly( "work2", "work3" );

		IndexFailureContext failureContext1 = failureContext1Capture.getValue();
		Assertions.assertThat( failureContext1.getThrowable() ).isSameAs( exception1 );
		Assertions.assertThat( failureContext1.getFailingOperation() )
				.isEqualTo( "work1" );
		Assertions.<Object>assertThat( failureContext1.getUncommittedOperations() )
				.containsExactly( "work1" );
	}

	/**
	 * Test that, when a sequence follows another one,
	 * but the first sequence is still executing when we start building the second one,
	 * everything works fine.
	 * <p>
	 * We used to have problems related to instance variables we referred to from lambdas:
	 * as these variables were reset when we started building the second sequence,
	 * the execution of the first sequence was relying on the wrong data,
	 * and in the worst case could even deadlock.
	 */
	@Test
	public void intertwinedSequenceExecution() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );

		Object work1Result = new Object();
		Object work2Result = new Object();
		Object work3Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> sequence1PreviousFuture = new CompletableFuture<>();
		CompletableFuture<?> sequence2PreviousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence1RefreshFuture = new CompletableFuture<>();
		CompletableFuture<Void> sequence2RefreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, failureHandlerMock );
		verifyAll();

		// Build and start the first sequence and simulate a long-running first work
		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		builder.init( sequence1PreviousFuture );
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequence1Future = builder.build();
		sequence1PreviousFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequence1Future ).isPending();

		// Meanwhile, build and start the second sequence
		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( work3.execute( contextMock ) ).andReturn( (CompletableFuture) work3Future );
		replayAll();
		builder.init( sequence2PreviousFuture );
		work3FutureFromSequenceBuilder = builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequence2Future = builder.build();
		sequence2PreviousFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequence1Future ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the first and second works
		resetAll();
		expect( work2.execute( contextMock ) ).andReturn( (CompletableFuture) work2Future );
		expect( contextMock.executePendingRefreshes() ).andReturn( sequence1RefreshFuture );
		replayAll();
		work1Future.complete( work1Result );
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( sequence1Future ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the refresh for the first sequence
		resetAll();
		replayAll();
		sequence1RefreshFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		// This used to fail because we didn't refer to the refresh future from the right sequence
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( sequence1Future ).isSuccessful( (Void) null );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the third work
		resetAll();
		expect( contextMock.executePendingRefreshes() ).andReturn( sequence2RefreshFuture );
		replayAll();
		work3Future.complete( null );
		verifyAll();
		assertThat( work3FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the refresh for the second sequence
		resetAll();
		replayAll();
		sequence2RefreshFuture.complete( null );
		verifyAll();
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThat( sequence2Future ).isSuccessful( (Void) null );
	}

	private <T> ElasticsearchWork<T> work(int index) {
		ElasticsearchWork<T> mock = createStrictMock( "work" + index, ElasticsearchWork.class );
		return mock;
	}

	private <T> BulkableElasticsearchWork<T> bulkableWork(int index) {
		BulkableElasticsearchWork<T> mock = createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
		return mock;
	}

	private static class MyException extends Exception {
	}

	private static class MyRuntimeException extends RuntimeException {
	}
}
