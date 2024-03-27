/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@SuppressWarnings("unchecked") // Raw types are the only way to mock parameterized types
class ElasticsearchDefaultWorkBulkerTest {

	private static final int DEFAULT_MAX_BULK_SIZE = 10;

	private static final DocumentRefreshStrategy DEFAULT_REFRESH = DocumentRefreshStrategy.NONE;

	@Mock
	private ElasticsearchWorkSequenceBuilder sequenceBuilderMock;
	@Mock
	private BiFunction<List<? extends BulkableWork<?>>,
			DocumentRefreshStrategy,
			NonBulkableWork<BulkResult>> bulkWorkFactoryMock;

	@Test
	void simple() {
		BulkableWork<Void> work1 = bulkableWorkMock( 1 );
		BulkableWork<Void> work2 = bulkableWorkMock( 2 );
		NonBulkableWork<BulkResult> bulkWork = workMock( 3 );

		CompletableFuture<Void> work1Future;
		CompletableFuture<Void> work1FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work2Future;
		CompletableFuture<Void> work2FutureFromSequenceBuilder = new CompletableFuture<>();
		ArgumentCaptor<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWorkFutureArgumentCaptor = futureCaptor();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock, DEFAULT_MAX_BULK_SIZE );
		verifyNoOtherSequenceInteractionsAndReset();

		when( work1.getRefreshStrategy() ).thenReturn( DEFAULT_REFRESH );
		when( sequenceBuilderMock.addBulkExecution( bulkWorkFutureArgumentCaptor.capture() ) )
				.thenReturn( bulkWorkResultFuture );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWorkResultFuture, work1, 0 ) )
				.thenReturn( work1FutureFromSequenceBuilder );
		work1Future = bulker.add( work1 );
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( work1Future ).isPending();
		assertThatFuture( bulkWorkFutureArgumentCaptor.getValue() ).isPending();

		when( work2.getRefreshStrategy() ).thenReturn( DEFAULT_REFRESH );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWorkResultFuture, work2, 1 ) )
				.thenReturn( work2FutureFromSequenceBuilder );
		work2Future = bulker.add( work2 );
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( work1Future ).isPending();
		assertThatFuture( work2Future ).isPending();
		assertThatFuture( bulkWorkFutureArgumentCaptor.getValue() ).isPending();

		when( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2 ), DEFAULT_REFRESH ) ).thenReturn( bulkWork );
		bulker.finalizeBulkWork();
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( work1Future ).isPending();
		assertThatFuture( work2Future ).isPending();
		assertThatFuture( bulkWorkFutureArgumentCaptor.getValue() ).isSuccessful( bulkWork );

		// Check that per-work futures are correctly bound to the futures returned by the sequence builder
		work1FutureFromSequenceBuilder.complete( null );
		assertThatFuture( work1Future ).isSuccessful( (Void) null );
		work2FutureFromSequenceBuilder.completeExceptionally( new RuntimeException() );
		assertThatFuture( work2Future ).isFailed();
		verifyNoOtherSequenceInteractionsAndReset();
	}

	@Test
	void alwaysBulk() {
		BulkableWork<Void> work1 = bulkableWorkMock( 1 );
		NonBulkableWork<BulkResult> bulkWork = workMock( 2 );

		CompletableFuture<Void> work1FutureFromSequenceBuilder = new CompletableFuture<>();
		ArgumentCaptor<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWorkFutureArgumentCaptor = futureCaptor();
		CompletableFuture<BulkResult> bulkWorkResultFuture = new CompletableFuture<>();

		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock, DEFAULT_MAX_BULK_SIZE );
		verifyNoOtherSequenceInteractionsAndReset();

		when( work1.getRefreshStrategy() ).thenReturn( DEFAULT_REFRESH );
		when( sequenceBuilderMock.addBulkExecution( bulkWorkFutureArgumentCaptor.capture() ) )
				.thenReturn( bulkWorkResultFuture );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWorkResultFuture, work1, 0 ) )
				.thenReturn( work1FutureFromSequenceBuilder );
		bulker.add( work1 );
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( bulkWorkFutureArgumentCaptor.getValue() ).isPending();

		when( bulkWorkFactoryMock.apply( Arrays.asList( work1 ), DEFAULT_REFRESH ) ).thenReturn( bulkWork );
		bulker.finalizeBulkWork();
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( bulkWorkFutureArgumentCaptor.getValue() ).isSuccessful( bulkWork );
	}

	@Test
	void newBulkOnTooManyBulkedWorks() {
		List<BulkableWork<Void>> firstBulkWorks = new ArrayList<>();
		for ( int i = 0; i < DEFAULT_MAX_BULK_SIZE; ++i ) {
			firstBulkWorks.add( bulkableWorkMock( i ) );
		}
		BulkableWork<Void> additionalWork1 = bulkableWorkMock( DEFAULT_MAX_BULK_SIZE );
		BulkableWork<Void> additionalWork2 = bulkableWorkMock( DEFAULT_MAX_BULK_SIZE + 1 );
		NonBulkableWork<BulkResult> bulkWork1 = workMock( DEFAULT_MAX_BULK_SIZE + 2 );
		NonBulkableWork<BulkResult> bulkWork2 = workMock( DEFAULT_MAX_BULK_SIZE + 3 );

		List<CompletableFuture<Void>> firstBulkWorksCompletableFuturesFromSequenceBuilder = new ArrayList<>();
		for ( int i = 0; i < DEFAULT_MAX_BULK_SIZE; ++i ) {
			firstBulkWorksCompletableFuturesFromSequenceBuilder.add( new CompletableFuture<>() );
		}
		CompletableFuture<Void> additionalWork1FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> additionalWork2FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork1ResultFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork2ResultFuture = new CompletableFuture<>();
		ArgumentCaptor<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork1FutureArgumentCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork2FutureArgumentCaptor = futureCaptor();

		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock, DEFAULT_MAX_BULK_SIZE );
		verifyNoOtherSequenceInteractionsAndReset();

		when( sequenceBuilderMock.addBulkExecution( bulkWork1FutureArgumentCaptor.capture() ) )
				.thenReturn( bulkWork1ResultFuture );
		for ( int i = 0; i < DEFAULT_MAX_BULK_SIZE; ++i ) {
			BulkableWork<Void> work = firstBulkWorks.get( i );
			when( work.getRefreshStrategy() ).thenReturn( DEFAULT_REFRESH );
			when( sequenceBuilderMock.addBulkResultExtraction( bulkWork1ResultFuture, work, i ) )
					.thenReturn( firstBulkWorksCompletableFuturesFromSequenceBuilder.get( i ) );
		}
		when( bulkWorkFactoryMock.apply( firstBulkWorks, DEFAULT_REFRESH ) ).thenReturn( bulkWork1 );
		for ( BulkableWork<?> work : firstBulkWorks ) {
			bulker.add( work );
		}
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( bulkWork1FutureArgumentCaptor.getValue() ).isSuccessful( bulkWork1 );

		when( additionalWork1.getRefreshStrategy() ).thenReturn( DEFAULT_REFRESH );
		when( sequenceBuilderMock.addBulkExecution( bulkWork2FutureArgumentCaptor.capture() ) )
				.thenReturn( bulkWork2ResultFuture );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWork2ResultFuture, additionalWork1, 0 ) )
				.thenReturn( additionalWork1FutureFromSequenceBuilder );
		bulker.add( additionalWork1 );
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( bulkWork2FutureArgumentCaptor.getValue() ).isPending();

		when( additionalWork2.getRefreshStrategy() ).thenReturn( DEFAULT_REFRESH );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWork2ResultFuture, additionalWork2, 1 ) )
				.thenReturn( additionalWork2FutureFromSequenceBuilder );
		bulker.add( additionalWork2 );
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( bulkWork2FutureArgumentCaptor.getValue() ).isPending();

		when( bulkWorkFactoryMock.apply( Arrays.asList( additionalWork1, additionalWork2 ), DEFAULT_REFRESH ) ).thenReturn(
				bulkWork2 );
		bulker.finalizeBulkWork();
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( bulkWork2FutureArgumentCaptor.getValue() ).isSuccessful( bulkWork2 );
	}

	@Test
	void newBulkOnDifferentRefresh() {
		BulkableWork<Void> work1 = bulkableWorkMock( 1 );
		BulkableWork<Void> work2 = bulkableWorkMock( 2 );
		BulkableWork<Void> work3 = bulkableWorkMock( 3 );
		BulkableWork<Void> work4 = bulkableWorkMock( 4 );
		BulkableWork<Void> work5 = bulkableWorkMock( 5 );
		NonBulkableWork<BulkResult> bulkWork1 = workMock( 7 );
		NonBulkableWork<BulkResult> bulkWork2 = workMock( 8 );
		NonBulkableWork<BulkResult> bulkWork3 = workMock( 9 );

		CompletableFuture<Void> work1FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work2FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work3FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work4FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<Void> work5FutureFromSequenceBuilder = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork1ResultFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork2ResultFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkWork3ResultFuture = new CompletableFuture<>();
		ArgumentCaptor<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork1FutureArgumentCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork2FutureArgumentCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<NonBulkableWork<BulkResult>>> bulkWork3FutureArgumentCaptor = futureCaptor();

		ElasticsearchDefaultWorkBulker bulker =
				new ElasticsearchDefaultWorkBulker( sequenceBuilderMock, bulkWorkFactoryMock, 10 );
		verifyNoOtherSequenceInteractionsAndReset();

		when( work1.getRefreshStrategy() ).thenReturn( DocumentRefreshStrategy.NONE );
		when( sequenceBuilderMock.addBulkExecution( bulkWork1FutureArgumentCaptor.capture() ) )
				.thenReturn( bulkWork1ResultFuture );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWork1ResultFuture, work1, 0 ) )
				.thenReturn( work1FutureFromSequenceBuilder );
		bulker.add( work1 );
		verifyNoOtherSequenceInteractionsAndReset();

		when( work2.getRefreshStrategy() ).thenReturn( DocumentRefreshStrategy.NONE );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWork1ResultFuture, work2, 1 ) )
				.thenReturn( work2FutureFromSequenceBuilder );
		bulker.add( work2 );
		verifyNoOtherSequenceInteractionsAndReset();

		// ForceRefresh from NONE to FORCE => new bulk
		when( work3.getRefreshStrategy() ).thenReturn( DocumentRefreshStrategy.FORCE );
		when( bulkWorkFactoryMock.apply( Arrays.asList( work1, work2 ), DocumentRefreshStrategy.NONE ) ).thenReturn(
				bulkWork1 );
		when( sequenceBuilderMock.addBulkExecution( bulkWork2FutureArgumentCaptor.capture() ) )
				.thenReturn( bulkWork2ResultFuture );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWork2ResultFuture, work3, 0 ) )
				.thenReturn( work3FutureFromSequenceBuilder );
		bulker.add( work3 );
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( bulkWork1FutureArgumentCaptor.getValue() ).isSuccessful( bulkWork1 );

		when( work4.getRefreshStrategy() ).thenReturn( DocumentRefreshStrategy.FORCE );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWork2ResultFuture, work4, 1 ) )
				.thenReturn( work4FutureFromSequenceBuilder );
		bulker.add( work4 );
		verifyNoOtherSequenceInteractionsAndReset();

		// ForceRefresh from FORCE to NONE => new bulk
		when( work5.getRefreshStrategy() ).thenReturn( DocumentRefreshStrategy.NONE );
		when( bulkWorkFactoryMock.apply( Arrays.asList( work3, work4 ), DocumentRefreshStrategy.FORCE ) ).thenReturn(
				bulkWork2 );
		when( sequenceBuilderMock.addBulkExecution( bulkWork3FutureArgumentCaptor.capture() ) )
				.thenReturn( bulkWork3ResultFuture );
		when( sequenceBuilderMock.addBulkResultExtraction( bulkWork3ResultFuture, work5, 0 ) )
				.thenReturn( work5FutureFromSequenceBuilder );
		bulker.add( work5 );
		verifyNoOtherSequenceInteractionsAndReset();
		assertThatFuture( bulkWork2FutureArgumentCaptor.getValue() ).isSuccessful( bulkWork2 );

		when( bulkWorkFactoryMock.apply( Arrays.asList( work5 ), DocumentRefreshStrategy.NONE ) ).thenReturn(
				bulkWork3 );
		bulker.finalizeBulkWork();
		verifyNoOtherSequenceInteractionsAndReset();
	}

	private void verifyNoOtherSequenceInteractionsAndReset() {
		verifyNoMoreInteractions( sequenceBuilderMock, bulkWorkFactoryMock );
		reset( sequenceBuilderMock, bulkWorkFactoryMock );
	}

	private <T> ArgumentCaptor<CompletableFuture<T>> futureCaptor() {
		return ArgumentCaptor.forClass( CompletableFuture.class );
	}

	private <T> NonBulkableWork<T> workMock(int index) {
		return mock( NonBulkableWork.class, "work" + index );
	}

	private <T> BulkableWork<T> bulkableWorkMock(int index) {
		return mock( BulkableWork.class, "bulkableWork" + index );
	}
}
