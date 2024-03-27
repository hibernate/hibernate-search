/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;

import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ElasticsearchBatchedWorkProcessorTest {

	/**
	 * @return A value that should not matter, because it should not be used.
	 */
	private static <T> T unusedReturnValue() {
		return null;
	}

	@Mock
	private ElasticsearchWorkSequenceBuilder sequenceBuilderMock;
	@Mock
	private ElasticsearchWorkBulker bulkerMock;

	@Test
	void simple_singleWork() {
		BulkableWork<Object> work = bulkableWorkMock( 1 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		ElasticsearchBatchedWorkProcessor processor =
				new ElasticsearchBatchedWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyNoOtherInteractionsAndReset();

		processor.beginBatch();
		verify( bulkerMock ).reset();
		verify( sequenceBuilderMock ).init( any() );
		verifyNoOtherInteractionsAndReset();

		CompletableFuture<Object> workFuture = new CompletableFuture<>();
		when( bulkerMock.add( work ) ).thenReturn( workFuture );
		CompletableFuture<Object> returnedWorkFuture = processor.submit( work );
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( returnedWorkFuture ).isSameAs( workFuture );

		when( sequenceBuilderMock.build() ).thenReturn( sequenceFuture );
		CompletableFuture<Void> batchFuture = processor.endBatch();
		verify( bulkerMock ).finalizeBulkWork();
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( batchFuture ).isPending();

		sequenceFuture.complete( null );
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( batchFuture ).isSuccessful();

		checkComplete( processor );
	}

	@Test
	void simple_multipleWorks() {
		BulkableWork<Object> work1 = bulkableWorkMock( 1 );
		BulkableWork<Object> work2 = bulkableWorkMock( 2 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		ElasticsearchBatchedWorkProcessor processor =
				new ElasticsearchBatchedWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyNoOtherInteractionsAndReset();

		processor.beginBatch();
		verify( bulkerMock ).reset();
		verify( sequenceBuilderMock ).init( any() );
		verifyNoOtherInteractionsAndReset();

		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		when( bulkerMock.add( work1 ) ).thenReturn( work1Future );
		when( bulkerMock.add( work2 ) ).thenReturn( work2Future );
		CompletableFuture<Object> returnedWork1Future = processor.submit( work1 );
		CompletableFuture<Object> returnedWork2Future = processor.submit( work2 );
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( returnedWork1Future ).isSameAs( work1Future );
		assertThatFuture( returnedWork2Future ).isSameAs( work2Future );

		when( sequenceBuilderMock.build() ).thenReturn( sequenceFuture );
		CompletableFuture<Void> batchFuture = processor.endBatch();
		verify( bulkerMock ).finalizeBulkWork();
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( batchFuture ).isPending();

		sequenceFuture.complete( null );
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( batchFuture ).isSuccessful();

		checkComplete( processor );
	}

	@Test
	void newSequenceBetweenBatches() {
		BulkableWork<Object> work1 = bulkableWorkMock( 1 );

		BulkableWork<Object> work2 = bulkableWorkMock( 2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		ElasticsearchBatchedWorkProcessor processor =
				new ElasticsearchBatchedWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyNoOtherInteractionsAndReset();

		processor.beginBatch();
		verify( bulkerMock ).reset();
		verify( sequenceBuilderMock ).init( any() );
		verifyNoOtherInteractionsAndReset();

		when( bulkerMock.add( work1 ) ).thenReturn( unusedReturnValue() );
		processor.submit( work1 );
		verifyNoOtherInteractionsAndReset();

		when( sequenceBuilderMock.build() ).thenReturn( sequence1Future );
		CompletableFuture<Void> batch1Future = processor.endBatch();
		verify( bulkerMock ).finalizeBulkWork();
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( batch1Future ).isPending();

		sequence1Future.complete( null );
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( batch1Future ).isSuccessful();

		processor.beginBatch();
		verify( bulkerMock ).reset();
		verify( sequenceBuilderMock ).init( any() );
		verifyNoOtherInteractionsAndReset();

		when( bulkerMock.add( work2 ) ).thenReturn( unusedReturnValue() );
		processor.submit( work2 );
		verifyNoOtherInteractionsAndReset();

		when( sequenceBuilderMock.build() ).thenReturn( sequence2Future );
		CompletableFuture<Void> batch2Future = processor.endBatch();
		verify( bulkerMock ).finalizeBulkWork();
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( batch2Future ).isPending();

		sequence2Future.complete( null );
		verifyNoOtherInteractionsAndReset();
		assertThatFuture( batch2Future ).isSuccessful();

		checkComplete( processor );
	}

	private void verifyNoOtherInteractionsAndReset() {
		verifyNoMoreInteractions( sequenceBuilderMock, bulkerMock );
		reset( sequenceBuilderMock, bulkerMock );
	}

	private void checkComplete(ElasticsearchBatchedWorkProcessor processor) {
		processor.complete();
		verifyNoOtherInteractionsAndReset();
	}

	@SuppressWarnings("unchecked") // Raw types are the only way to mock parameterized types
	private <T> BulkableWork<T> bulkableWorkMock(int index) {
		return mock( BulkableWork.class, "bulkableWork" + index );
	}
}
