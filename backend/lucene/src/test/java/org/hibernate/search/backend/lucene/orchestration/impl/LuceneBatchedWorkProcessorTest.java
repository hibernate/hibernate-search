/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.work.impl.IndexingWork;
import org.hibernate.search.backend.lucene.work.impl.IndexingWorkExecutionContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LuceneBatchedWorkProcessorTest {

	private static final String INDEX_NAME = "SomeIndexName";

	private final EventContext indexEventContext = EventContexts.fromIndexName( INDEX_NAME );

	@Mock
	private IndexAccessor indexAccessorMock;
	@Mock
	private IndexWriterDelegator indexWriterDelegatorMock;

	private LuceneBatchedWorkProcessor processor;

	private int nextWorkId = 0;

	@BeforeEach
	void setup() {
		processor = new LuceneBatchedWorkProcessor( indexEventContext, indexAccessorMock );
	}

	@Test
	void batch() throws IOException {
		processor.beginBatch();
		verifyNoOtherIndexInteractionsAndClear();

		testSuccessfulWriteWorks( 200 );
		verifyNoOtherIndexInteractionsAndClear();

		verify( indexAccessorMock, never() ).commitOrDelay();
		processor.endBatch();
		// Give a chance to the I/O strategy to schedule a delayed commit.
		verify( indexAccessorMock ).commitOrDelay();
		verifyNoOtherIndexInteractionsAndClear();

		processor.beginBatch();
		verifyNoOtherIndexInteractionsAndClear();

		testSuccessfulWriteWorks( 100 );
		verifyNoOtherIndexInteractionsAndClear();

		verify( indexAccessorMock, never() ).commitOrDelay();
		processor.endBatch();
		// Give a chance to the I/O strategy to schedule a delayed commit.
		verify( indexAccessorMock ).commitOrDelay();
		verifyNoOtherIndexInteractionsAndClear();

		checkCompleteWithNothingToCommit();
	}

	@Test
	void error_workExecute() throws IOException {
		processor.beginBatch();
		verifyNoOtherIndexInteractionsAndClear();

		// Execute a few successful works
		testSuccessfulWriteWorks( 50 );
		verifyNoOtherIndexInteractionsAndClear();

		// ... and suddenly a failing work
		RuntimeException workException = new RuntimeException( "Some message" );
		IndexingWork<Object> failingWork = workMock();
		when( failingWork.execute( any() ) ).thenThrow( workException );
		assertThatThrownBy( () -> processor.submit( failingWork ) )
				.isSameAs( workException );
		verify( indexAccessorMock ).cleanUpAfterFailure( workException, workInfo( 50 ) );
		verifyNoOtherIndexInteractionsAndClear();

		// Subsequent works must be executed regardless of previous failures in the same batch
		testSuccessfulWriteWorks( 10 );
		verifyNoOtherIndexInteractionsAndClear();

		processor.endBatch();
		verify( indexAccessorMock ).commitOrDelay();
		verifyNoOtherIndexInteractionsAndClear();

		checkCompleteWithNothingToCommit();
	}

	@Test
	void forceCommit() {
		processor.forceCommit();

		verify( indexAccessorMock ).commit();
		verifyNoOtherIndexInteractionsAndClear();
	}

	@Test
	void error_forceCommit() {
		RuntimeException commitException = new RuntimeException( "Some message" );
		doThrow( commitException ).when( indexAccessorMock ).commit();
		assertThatThrownBy( () -> processor.forceCommit() )
				.isSameAs( commitException );
		verify( indexAccessorMock ).cleanUpAfterFailure( commitException, "Commit after a set of index works" );
		verifyNoOtherIndexInteractionsAndClear();

		processor.endBatch();
		verify( indexAccessorMock ).commitOrDelay();
		verifyNoOtherIndexInteractionsAndClear();

		checkCompleteWithNothingToCommit();
	}

	@Test
	void forceRefresh() {
		processor.forceRefresh();
		verify( indexAccessorMock ).refresh();
		verifyNoOtherIndexInteractionsAndClear();
	}

	@Test
	void error_forceRefresh() {
		RuntimeException refreshException = new RuntimeException( "Some message" );
		doThrow( refreshException ).when( indexAccessorMock ).refresh();
		assertThatThrownBy( () -> processor.forceRefresh() )
				.isSameAs( refreshException );
		verifyNoOtherIndexInteractionsAndClear();
	}

	@Test
	void error_batchCommit() throws IOException {
		RuntimeException commitException = new RuntimeException( "Some message" );

		processor.beginBatch();
		verifyNoOtherIndexInteractionsAndClear();

		// Execute a few successful works
		testSuccessfulWriteWorks( 200 );
		verifyNoOtherIndexInteractionsAndClear();

		// Fail upon batch commit
		doThrow( commitException ).when( indexAccessorMock ).commitOrDelay();
		processor.endBatch();
		verify( indexAccessorMock )
				.cleanUpAfterFailure( commitException, "Commit after a batch of index works" );
		verifyNoOtherIndexInteractionsAndClear();

		checkCompleteWithNothingToCommit();
	}

	private void testSuccessfulWriteWorks(int workCount) throws IOException {
		ArgumentCaptor<IndexingWorkExecutionContext> contextCapture =
				ArgumentCaptor.forClass( IndexingWorkExecutionContext.class );

		when( indexAccessorMock.getIndexWriterDelegator() ).thenReturn( indexWriterDelegatorMock );

		for ( int i = 0; i < workCount; ++i ) {
			IndexingWork<Object> work = workMock();
			Object workResult = new Object();

			when( work.execute( contextCapture.capture() ) ).thenReturn( workResult );

			assertThat( processor.submit( work ) ).isEqualTo( workResult );

			testContext( contextCapture.getValue() );
		}
	}

	private void checkCompleteWithNothingToCommit() {
		doNothing().when( indexAccessorMock ).commitOrDelay();

		processor.complete();

		// The index accessor (or the underlying writer) is responsible for detecting there is nothing to commit.
		verify( indexAccessorMock ).commitOrDelay();
		verifyNoOtherIndexInteractionsAndClear();
	}

	private void testContext(IndexingWorkExecutionContext context) throws IOException {
		assertThat( context.getEventContext() ).isSameAs( indexEventContext );

		assertThat( context.getIndexWriterDelegator() ).isSameAs( indexWriterDelegatorMock );
	}

	private void verifyNoOtherIndexInteractionsAndClear() {
		verifyNoMoreInteractions( indexAccessorMock, indexWriterDelegatorMock );
		clearInvocations( indexAccessorMock, indexWriterDelegatorMock );
	}

	@SuppressWarnings("unchecked") // Raw types are the only way to mock parameterized types
	private <T> IndexingWork<T> workMock() {
		int id = nextWorkId++;
		String workName = workInfo( id );
		IndexingWork<T> workMock = mock( IndexingWork.class,
				withSettings().name( workName ).strictness( Strictness.LENIENT ) );
		when( workMock.getInfo() ).thenReturn( workInfo( id ) );
		return workMock;
	}

	private String workInfo(int index) {
		return "work_" + index;
	}

}
