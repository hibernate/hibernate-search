/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.easymock.EasyMock.expect;
import static org.hibernate.search.util.impl.test.ExceptionMatcherBuilder.isException;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.util.common.SearchException;

import org.junit.Before;
import org.junit.Test;

import org.easymock.EasyMockSupport;


@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class ElasticsearchDefaultWorkSequenceBuilderTest extends EasyMockSupport {

	private ElasticsearchWorkExecutionContext contextMock;

	@Before
	public void initMocks() {
		contextMock = createStrictMock( ElasticsearchWorkExecutionContext.class );
	}

	@Test
	public void simple() {
		NonBulkableWork<Object> work1 = work( 1 );
		NonBulkableWork<Object> work2 = work( 2 );

		Object work1Result = new Object();
		Object work2Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
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
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	public void bulk() {
		NonBulkableWork<Object> work1 = work( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );
		NonBulkableWork<Object> work4 = work( 4 );
		NonBulkableWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work2Result = new Object();
		Object work3Result = new Object();
		Object work4Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<NonBulkableWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work4Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Object> work4FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 0 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 1 );
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
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
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
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.extract( contextMock, work2, 0 ) ).andReturn( work2Result );
		expect( bulkResultMock.extract( contextMock, work3, 1 ) ).andReturn( work3Result );
		expect( work4.execute( contextMock ) ).andReturn( (CompletableFuture) work4Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work4Future.complete( work4Result );
		verifyAll();
		assertThat( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		assertThat( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	public void newSequenceOnReset() {
		NonBulkableWork<Void> work1 = work( 1 );
		NonBulkableWork<Void> work2 = work( 2 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture1 = new CompletableFuture<>();
		CompletableFuture<?> previousFuture2 = new CompletableFuture<>();
		// We'll never complete this future, so as to check that work 2 is executed in a different sequence
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
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
		replayAll();
		work2Future.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isSuccessful( (Void) null );
	}

	@Test
	public void error_work() {
		NonBulkableWork<Object> work0 = work( 0 );
		NonBulkableWork<Void> work1 = work( 1 );
		NonBulkableWork<Object> work2 = work( 2 );

		Object work0Result = new Object();
		Object work2Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work0FutureFromSequenceBuilder;
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;

		MyException exception = new MyException();

		expect( work0.execute( contextMock ) ).andReturn( (CompletableFuture) work0Future );
		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
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
		// Works that happened before the error must be considered as successful
		assertThat( work0FutureFromSequenceBuilder ).isSuccessful( work0Result );
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		// Subsequent works should be executed even if previous works failed
		expect( work2.execute( contextMock ) ).andReturn( work2Future );
		replayAll();
		work1Future.completeExceptionally( exception );
		verifyAll();
		// Errors must be propagated to the individual work futures ASAP
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		// Work failures should not be propagated to the sequence future
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_work() {
		BulkableWork<Void> work1 = bulkableWork( 1 );
		BulkableWork<Void> work2 = bulkableWork( 2 );
		BulkableWork<Void> work3 = bulkableWork( 3 );
		NonBulkableWork<Object> work4 = work( 4 );

		Object work4Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<NonBulkableWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<Object> work4Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Void> work3FutureFromSequenceBuilder;
		CompletableFuture<Object> work4FutureFromSequenceBuilder;

		MyException exception = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 2 );
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
		// Subsequent works should be executed even if previous works failed
		expect( work4.execute( contextMock ) ).andReturn( work4Future );
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
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work4Future.complete( work4Result );
		verifyAll();
		assertThat( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		// Work failures should not be propagated to the sequence future
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_result() {
		BulkableWork<Void> work1 = bulkableWork( 1 );
		BulkableWork<Void> work2 = bulkableWork( 2 );
		BulkableWork<Void> work3 = bulkableWork( 3 );
		NonBulkableWork<BulkResult> bulkWork = work( 4 );
		NonBulkableWork<Object> work4 = work( 5 );

		Object work4Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<NonBulkableWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work4Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Void> work3FutureFromSequenceBuilder;
		CompletableFuture<Object> work4FutureFromSequenceBuilder;

		MyException exception = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 2 );
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
		// Subsequent works should be executed even if previous works failed
		expect( work4.execute( contextMock ) ).andReturn( work4Future );
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
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work4Future.complete( work4Result );
		verifyAll();
		assertThat( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		// Work failures should not be propagated to the sequence future
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_resultExtraction_singleFailure() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Void> work2 = bulkableWork( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );
		NonBulkableWork<Object> work4 = work( 4 );
		NonBulkableWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work3Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		Object work4Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<NonBulkableWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work4Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Object> work4FutureFromSequenceBuilder;

		MyRuntimeException exception = new MyRuntimeException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 2 );
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
		expect( bulkResultMock.extract( contextMock, work1, 0 ) ).andReturn( work1Result );
		expect( bulkResultMock.extract( contextMock, work2, 1 ) ).andThrow( exception );
		expect( bulkResultMock.extract( contextMock, work3, 2 ) ).andReturn( work3Result );
		// Subsequent works should be executed even if previous works failed
		expect( work4.execute( contextMock ) ).andReturn( work4Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work4Future.complete( work4Result );
		verifyAll();
		assertThat( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		// Work failures should not be propagated to the sequence future
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_resultExtraction_multipleFailures() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );
		NonBulkableWork<Object> work3 = work( 3 );
		NonBulkableWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		Object work3Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<NonBulkableWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;

		MyRuntimeException exception1 = new MyRuntimeException();
		MyRuntimeException exception2 = new MyRuntimeException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
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
		expect( bulkResultMock.extract( contextMock, work1, 0 ) ).andThrow( exception1 );
		expect( bulkResultMock.extract( contextMock, work2, 1 ) ).andThrow( exception2 );
		// Subsequent works should be executed even if previous works failed
		expect( work3.execute( contextMock ) ).andReturn( work3Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception1 );
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception2 );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work3Future.complete( work3Result );
		verifyAll();
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		// Work failures should not be propagated to the sequence future
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_resultExtraction_future_singleFailure() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Void> work2 = bulkableWork( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );
		NonBulkableWork<Object> work4 = work( 4 );
		NonBulkableWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work3Result = new Object();
		Object work4Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<NonBulkableWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work4Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Object> work4FutureFromSequenceBuilder;

		MyRuntimeException exception = new MyRuntimeException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 2 );
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
		expect( bulkResultMock.extract( contextMock, work1, 0 ) ).andReturn( work1Result );
		expect( bulkResultMock.extract( contextMock, work2, 1 ) ).andThrow( exception );
		expect( bulkResultMock.extract( contextMock, work3, 2 ) ).andReturn( work3Result );
		// Subsequent works should be executed even if previous works failed
		expect( work4.execute( contextMock ) ).andReturn( work4Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work4Future.complete( work4Result );
		verifyAll();
		assertThat( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		// Work failures should not be propagated to the sequence future
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_resultExtraction_future_multipleFailures() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );
		NonBulkableWork<Object> work3 = work( 3 );
		NonBulkableWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		Object work3Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<NonBulkableWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;

		MyRuntimeException exception1 = new MyRuntimeException();
		MyRuntimeException exception2 = new MyRuntimeException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		resetAll();
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
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
		expect( bulkResultMock.extract( contextMock, work1, 0 ) ).andThrow( exception1 );
		expect( bulkResultMock.extract( contextMock, work2, 1 ) ).andThrow( exception2 );
		// Subsequent works should be executed even if previous works failed
		expect( work3.execute( contextMock ) ).andReturn( work3Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception1 );
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception2 );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work3Future.complete( work3Result );
		verifyAll();
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		// Work failures should not be propagated to the sequence future
		assertThat( sequenceFuture ).isSuccessful();
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
		NonBulkableWork<Object> work1 = work( 1 );
		NonBulkableWork<Object> work2 = work( 2 );
		NonBulkableWork<Object> work3 = work( 3 );

		Object work1Result = new Object();
		Object work2Result = new Object();
		Object work3Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> sequence1PreviousFuture = new CompletableFuture<>();
		CompletableFuture<?> sequence2PreviousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyAll();

		// Build and start the first sequence and simulate a long-running first work
		resetAll();
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
		replayAll();
		work1Future.complete( work1Result );
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		// This used to fail when we had end-of-sequecne refreshes,
		// because we didn't refer to the refresh future from the right sequence
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( sequence1Future ).isSuccessful( (Void) null );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the third work
		resetAll();
		replayAll();
		work3Future.complete( null );
		verifyAll();
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThat( sequence2Future ).isSuccessful( (Void) null );
	}

	private <T> NonBulkableWork<T> work(int index) {
		return createStrictMock( "work" + index, NonBulkableWork.class );
	}

	private <T> BulkableWork<T> bulkableWork(int index) {
		return createStrictMock( "bulkableWork" + index, BulkableWork.class );
	}

	private static class MyException extends Exception {
	}

	private static class MyRuntimeException extends RuntimeException {
	}
}
