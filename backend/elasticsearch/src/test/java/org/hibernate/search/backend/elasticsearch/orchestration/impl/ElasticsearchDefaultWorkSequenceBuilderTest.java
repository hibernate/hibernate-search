/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@SuppressWarnings({ "unchecked", "rawtypes" }) // Raw types are the only way to mock parameterized types
class ElasticsearchDefaultWorkSequenceBuilderTest {

	@Mock
	private ElasticsearchWorkExecutionContext contextMock;

	private final List<Object> mocks = new ArrayList<>();

	@Test
	void simple() {
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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture );
		verifyNoOtherWorkInteractionsAndClear();

		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();

		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();

		CompletableFuture<Void> sequenceFuture = builder.build();
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( work1.execute( contextMock ) ).thenReturn( (CompletableFuture) work1Future );
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( work2.execute( contextMock ) ).thenReturn( (CompletableFuture) work2Future );
		work1Future.complete( work1Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work2Future.complete( work2Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThatFuture( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	void bulk() {
		NonBulkableWork<Object> work1 = work( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );
		NonBulkableWork<Object> work4 = work( 4 );
		NonBulkableWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work2Result = new Object();
		Object work3Result = new Object();
		Object work4Result = new Object();
		BulkResult bulkResultMock = bulkResultMock();

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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture );
		verifyNoOtherWorkInteractionsAndClear();

		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 0 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 1 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();

		CompletableFuture<Void> sequenceFuture = builder.build();
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( work1.execute( contextMock ) ).thenReturn( (CompletableFuture) work1Future );
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work1Future.complete( work1Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkWork.execute( contextMock ) ).thenReturn( (CompletableFuture) bulkResultFuture );
		bulkWorkFuture.complete( bulkWork );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkResultMock.extract( contextMock, work2, 0 ) ).thenReturn( work2Result );
		when( bulkResultMock.extract( contextMock, work3, 1 ) ).thenReturn( work3Result );
		when( work4.execute( contextMock ) ).thenReturn( (CompletableFuture) work4Future );
		bulkResultFuture.complete( bulkResultMock );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThatFuture( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThatFuture( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work4Future.complete( work4Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		assertThatFuture( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	void newSequenceOnReset() {
		NonBulkableWork<Void> work1 = work( 1 );
		NonBulkableWork<Void> work2 = work( 2 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture1 = new CompletableFuture<>();
		CompletableFuture<?> previousFuture2 = new CompletableFuture<>();
		// We'll never complete this future, so as to check that work 2 is executed in a different sequence
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture1 );
		verifyNoOtherWorkInteractionsAndClear();

		builder.addNonBulkExecution( work1 );
		verifyNoOtherWorkInteractionsAndClear();

		CompletableFuture<Void> sequence1Future = builder.build();
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequence1Future ).isPending();

		when( work1.execute( contextMock ) ).thenReturn( (CompletableFuture) work1Future );
		previousFuture1.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequence1Future ).isPending();

		builder.init( previousFuture2 );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequence1Future ).isPending();

		builder.addNonBulkExecution( work2 );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequence1Future ).isPending();

		CompletableFuture<Void> sequence2Future = builder.build();
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequence1Future ).isPending();
		assertThatFuture( sequence2Future ).isPending();

		when( work2.execute( contextMock ) ).thenReturn( (CompletableFuture) work2Future );
		previousFuture2.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequence1Future ).isPending();
		assertThatFuture( sequence2Future ).isPending();

		work2Future.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequence1Future ).isPending();
		assertThatFuture( sequence2Future ).isSuccessful( (Void) null );
	}

	@Test
	void error_work() {
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

		when( work0.execute( contextMock ) ).thenReturn( (CompletableFuture) work0Future );
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		builder.init( previousFuture );
		work0FutureFromSequenceBuilder = builder.addNonBulkExecution( work0 );
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work0FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( work1.execute( contextMock ) ).thenReturn( (CompletableFuture) work1Future );
		work0Future.complete( work0Result );
		verifyNoOtherWorkInteractionsAndClear();
		// Works that happened before the error must be considered as successful
		assertThatFuture( work0FutureFromSequenceBuilder ).isSuccessful( work0Result );
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		// Subsequent works should be executed even if previous works failed
		when( work2.execute( contextMock ) ).thenReturn( work2Future );
		work1Future.completeExceptionally( exception );
		verifyNoOtherWorkInteractionsAndClear();
		// Errors must be propagated to the individual work futures ASAP
		assertThatFuture( work1FutureFromSequenceBuilder ).isFailed( exception );
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work2Future.complete( work2Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		// Work failures should not be propagated to the sequence future
		assertThatFuture( sequenceFuture ).isSuccessful();
	}

	@Test
	void error_bulk_work() {
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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture );
		verifyNoOtherWorkInteractionsAndClear();

		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 2 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		// Subsequent works should be executed even if previous works failed
		when( work4.execute( contextMock ) ).thenReturn( work4Future );
		bulkWorkFuture.completeExceptionally( exception );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isFailed( exception );
		assertThatFuture( work1FutureFromSequenceBuilder ).getFailure()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Call to the bulk REST API failed" )
				.hasCauseReference( exception );
		assertThatFuture( work2FutureFromSequenceBuilder ).getFailure()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Call to the bulk REST API failed" )
				.hasCauseReference( exception );
		assertThatFuture( work3FutureFromSequenceBuilder ).getFailure()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Call to the bulk REST API failed" )
				.hasCauseReference( exception );
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work4Future.complete( work4Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		// Work failures should not be propagated to the sequence future
		assertThatFuture( sequenceFuture ).isSuccessful();
	}

	@Test
	void error_bulk_result() {
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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture );
		verifyNoOtherWorkInteractionsAndClear();

		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 2 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkWork.execute( contextMock ) ).thenReturn( (CompletableFuture) bulkResultFuture );
		bulkWorkFuture.complete( bulkWork );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		// Subsequent works should be executed even if previous works failed
		when( work4.execute( contextMock ) ).thenReturn( work4Future );
		bulkResultFuture.completeExceptionally( exception );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isFailed( exception );
		assertThatFuture( work1FutureFromSequenceBuilder ).getFailure()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Call to the bulk REST API failed" )
				.hasCauseReference( exception );
		assertThatFuture( work2FutureFromSequenceBuilder ).getFailure()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Call to the bulk REST API failed" )
				.hasCauseReference( exception );
		assertThatFuture( work3FutureFromSequenceBuilder ).getFailure()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Call to the bulk REST API failed" )
				.hasCauseReference( exception );
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work4Future.complete( work4Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		// Work failures should not be propagated to the sequence future
		assertThatFuture( sequenceFuture ).isSuccessful();
	}

	@Test
	void error_bulk_resultExtraction_singleFailure() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Void> work2 = bulkableWork( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );
		NonBulkableWork<Object> work4 = work( 4 );
		NonBulkableWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work3Result = new Object();
		BulkResult bulkResultMock = bulkResultMock();
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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture );
		verifyNoOtherWorkInteractionsAndClear();

		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 2 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkWork.execute( contextMock ) ).thenReturn( (CompletableFuture) bulkResultFuture );
		bulkWorkFuture.complete( bulkWork );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkResultMock.extract( contextMock, work1, 0 ) ).thenReturn( work1Result );
		when( bulkResultMock.extract( contextMock, work2, 1 ) ).thenThrow( exception );
		when( bulkResultMock.extract( contextMock, work3, 2 ) ).thenReturn( work3Result );
		// Subsequent works should be executed even if previous works failed
		when( work4.execute( contextMock ) ).thenReturn( work4Future );
		bulkResultFuture.complete( bulkResultMock );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThatFuture( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThatFuture( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThatFuture( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work4Future.complete( work4Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		// Work failures should not be propagated to the sequence future
		assertThatFuture( sequenceFuture ).isSuccessful();
	}

	@Test
	void error_bulk_resultExtraction_multipleFailures() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );
		NonBulkableWork<Object> work3 = work( 3 );
		NonBulkableWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = bulkResultMock();
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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture );
		verifyNoOtherWorkInteractionsAndClear();

		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkWork.execute( contextMock ) ).thenReturn( (CompletableFuture) bulkResultFuture );
		bulkWorkFuture.complete( bulkWork );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkResultMock.extract( contextMock, work1, 0 ) ).thenThrow( exception1 );
		when( bulkResultMock.extract( contextMock, work2, 1 ) ).thenThrow( exception2 );
		// Subsequent works should be executed even if previous works failed
		when( work3.execute( contextMock ) ).thenReturn( work3Future );
		bulkResultFuture.complete( bulkResultMock );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThatFuture( work1FutureFromSequenceBuilder ).isFailed( exception1 );
		assertThatFuture( work2FutureFromSequenceBuilder ).isFailed( exception2 );
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work3Future.complete( work3Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		// Work failures should not be propagated to the sequence future
		assertThatFuture( sequenceFuture ).isSuccessful();
	}

	@Test
	void error_bulk_resultExtraction_future_singleFailure() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Void> work2 = bulkableWork( 2 );
		BulkableWork<Object> work3 = bulkableWork( 3 );
		NonBulkableWork<Object> work4 = work( 4 );
		NonBulkableWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work3Result = new Object();
		Object work4Result = new Object();
		BulkResult bulkResultMock = bulkResultMock();

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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture );
		verifyNoOtherWorkInteractionsAndClear();

		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work3, 2 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkWork.execute( contextMock ) ).thenReturn( (CompletableFuture) bulkResultFuture );
		bulkWorkFuture.complete( bulkWork );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkResultMock.extract( contextMock, work1, 0 ) ).thenReturn( work1Result );
		when( bulkResultMock.extract( contextMock, work2, 1 ) ).thenThrow( exception );
		when( bulkResultMock.extract( contextMock, work3, 2 ) ).thenReturn( work3Result );
		// Subsequent works should be executed even if previous works failed
		when( work4.execute( contextMock ) ).thenReturn( work4Future );
		bulkResultFuture.complete( bulkResultMock );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThatFuture( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThatFuture( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThatFuture( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThatFuture( work4FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work4Future.complete( work4Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		// Work failures should not be propagated to the sequence future
		assertThatFuture( sequenceFuture ).isSuccessful();
	}

	@Test
	void error_bulk_resultExtraction_future_multipleFailures() {
		BulkableWork<Object> work1 = bulkableWork( 1 );
		BulkableWork<Object> work2 = bulkableWork( 2 );
		NonBulkableWork<Object> work3 = work( 3 );
		NonBulkableWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = bulkResultMock();
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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		builder.init( previousFuture );
		verifyNoOtherWorkInteractionsAndClear();

		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		work1FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work1, 0 );
		work2FutureFromSequenceBuilder = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture, work2, 1 );
		work3FutureFromSequenceBuilder = builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkWork.execute( contextMock ) ).thenReturn( (CompletableFuture) bulkResultFuture );
		bulkWorkFuture.complete( bulkWork );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isPending();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		when( bulkResultMock.extract( contextMock, work1, 0 ) ).thenThrow( exception1 );
		when( bulkResultMock.extract( contextMock, work2, 1 ) ).thenThrow( exception2 );
		// Subsequent works should be executed even if previous works failed
		when( work3.execute( contextMock ) ).thenReturn( work3Future );
		bulkResultFuture.complete( bulkResultMock );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThatFuture( work1FutureFromSequenceBuilder ).isFailed( exception1 );
		assertThatFuture( work2FutureFromSequenceBuilder ).isFailed( exception2 );
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequenceFuture ).isPending();

		work3Future.complete( work3Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		// Work failures should not be propagated to the sequence future
		assertThatFuture( sequenceFuture ).isSuccessful();
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
	void intertwinedSequenceExecution() {
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

		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder( contextMock );
		verifyNoOtherWorkInteractionsAndClear();

		// Build and start the first sequence and simulate a long-running first work
		when( work1.execute( contextMock ) ).thenReturn( (CompletableFuture) work1Future );
		builder.init( sequence1PreviousFuture );
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequence1Future = builder.build();
		sequence1PreviousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequence1Future ).isPending();

		// Meanwhile, build and start the second sequence
		when( work3.execute( contextMock ) ).thenReturn( (CompletableFuture) work3Future );
		builder.init( sequence2PreviousFuture );
		work3FutureFromSequenceBuilder = builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequence2Future = builder.build();
		sequence2PreviousFuture.complete( null );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work1FutureFromSequenceBuilder ).isPending();
		assertThatFuture( work2FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequence1Future ).isPending();
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequence2Future ).isPending();

		// Then simulate the end of the first and second works
		when( work2.execute( contextMock ) ).thenReturn( (CompletableFuture) work2Future );
		work1Future.complete( work1Result );
		work2Future.complete( work2Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		// This used to fail when we had end-of-sequecne refreshes,
		// because we didn't refer to the refresh future from the right sequence
		assertThatFuture( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThatFuture( sequence1Future ).isSuccessful( (Void) null );
		assertThatFuture( work3FutureFromSequenceBuilder ).isPending();
		assertThatFuture( sequence2Future ).isPending();

		// Then simulate the end of the third work
		work3Future.complete( work3Result );
		verifyNoOtherWorkInteractionsAndClear();
		assertThatFuture( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThatFuture( sequence2Future ).isSuccessful( (Void) null );
	}

	private void verifyNoOtherWorkInteractionsAndClear() {
		verifyNoMoreInteractions( mocks.toArray() );
		clearInvocations( mocks.toArray() );
	}

	private <T> NonBulkableWork<T> work(int index) {
		NonBulkableWork<T> workMock = mock( NonBulkableWork.class, "work" + index );
		mocks.add( workMock );
		return workMock;
	}

	private <T> BulkableWork<T> bulkableWork(int index) {
		BulkableWork<T> workMock = mock( BulkableWork.class, "bulkableWork" + index );
		mocks.add( workMock );
		return workMock;
	}

	private BulkResult bulkResultMock() {
		BulkResult mock = mock( BulkResult.class );
		mocks.add( mock );
		return mock;
	}

	private static class MyException extends Exception {
	}

	private static class MyRuntimeException extends RuntimeException {
	}
}
