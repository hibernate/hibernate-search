/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.AbstractThrowableAssert;

class BackendMockBackendIndexingWorkExpectationsTest {

	private static final String INDEX_NAME = "indexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4287")
	void syncDefaultAddWorkType() {
		backendMock.indexingWorkExpectations( BackendIndexingWorkExpectations.sync() );
		backendMock.expectWorks( INDEX_NAME ).executeFollowingWorks().add( b -> {} );
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork(
				INDEX_NAME,
				StubDocumentWork.builder( StubDocumentWork.Type.ADD )
						.commit( DocumentCommitStrategy.FORCE )
						.refresh( DocumentRefreshStrategy.NONE )
						.build()
		) )
				.doesNotThrowAnyException();

		backendMock.expectWorks( INDEX_NAME ).executeFollowingWorks().add( b -> {} );
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork(
				INDEX_NAME,
				StubDocumentWork.builder( StubDocumentWork.Type.ADD_OR_UPDATE )
						.commit( DocumentCommitStrategy.FORCE )
						.refresh( DocumentRefreshStrategy.NONE )
						.build()
		) )
				.isInstanceOf( AssertionError.class );
		backendMock.resetExpectations();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4287")
	void asyncDefaultAddWorkType() {
		backendMock.indexingWorkExpectations( BackendIndexingWorkExpectations.async( ".*" ) );
		backendMock.expectWorks( INDEX_NAME ).executeFollowingWorks().add( b -> {} );
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork(
				INDEX_NAME,
				StubDocumentWork.builder( StubDocumentWork.Type.ADD )
						.commit( DocumentCommitStrategy.FORCE )
						.refresh( DocumentRefreshStrategy.NONE )
						.build()
		) )
				.doesNotThrowAnyException();

		backendMock.expectWorks( INDEX_NAME ).executeFollowingWorks().add( b -> {} );
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork(
				INDEX_NAME,
				StubDocumentWork.builder( StubDocumentWork.Type.ADD_OR_UPDATE )
						.commit( DocumentCommitStrategy.FORCE )
						.refresh( DocumentRefreshStrategy.NONE )
						.build()
		) )
				.isInstanceOf( AssertionError.class );
		backendMock.resetExpectations();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4287")
	void asyncCustomAddWorkType() {
		// We tell the backend that all "add" expectations should instead be interpreted as "addOrUpdate",
		// because the mapper is configured to always issue ADD_OR_UPDATE operations.
		backendMock
				.indexingWorkExpectations( BackendIndexingWorkExpectations.async( ".*", StubDocumentWork.Type.ADD_OR_UPDATE ) );
		// We expect an "add" operation...
		backendMock.expectWorks( INDEX_NAME ).executeFollowingWorks().add( b -> {} );
		// We trigger an "addOrUpdate" operation...
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork(
				INDEX_NAME,
				StubDocumentWork.builder( StubDocumentWork.Type.ADD_OR_UPDATE )
						.commit( DocumentCommitStrategy.FORCE )
						.refresh( DocumentRefreshStrategy.NONE )
						.build()
		) )
				// And they should match anyway.
				.doesNotThrowAnyException();

		// We expect an "add" operation...
		backendMock.expectWorks( INDEX_NAME ).executeFollowingWorks().add( b -> {} );
		// We trigger a "delete" operation...
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork(
				INDEX_NAME,
				StubDocumentWork.builder( StubDocumentWork.Type.DELETE )
						.commit( DocumentCommitStrategy.FORCE )
						.refresh( DocumentRefreshStrategy.NONE )
						.build()
		) )
				// And they won't match.
				.isInstanceOf( AssertionError.class );
		backendMock.resetExpectations();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4133")
	void workSubmittedFromExpectedThread() {
		backendMock.indexingWorkExpectations( BackendIndexingWorkExpectations.async( "matching.*" ) );
		backendMock.expectWorks( INDEX_NAME ).executeFollowingWorks().add( b -> {} );
		assertThatThrownByCodeRunningInThreadWithName( "matchingFoo",
				() -> {
					backendMock.backendBehavior().executeDocumentWork( INDEX_NAME,
							StubDocumentWork.builder( StubDocumentWork.Type.ADD )
									.commit( DocumentCommitStrategy.FORCE )
									.refresh( DocumentRefreshStrategy.NONE )
									.build() );
				} )
				.doesNotThrowAnyException();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4133")
	void workSubmittedFromUnexpectedThread() {
		backendMock.indexingWorkExpectations( BackendIndexingWorkExpectations.async( "matching.*" ) );
		backendMock.expectWorks( INDEX_NAME ).executeFollowingWorks().add( b -> {} );
		assertThatThrownByCodeRunningInThreadWithName( "wrongName",
				() -> {
					backendMock.backendBehavior().executeDocumentWork( INDEX_NAME,
							StubDocumentWork.builder( StubDocumentWork.Type.ADD )
									.commit( DocumentCommitStrategy.FORCE )
									.refresh( DocumentRefreshStrategy.NONE )
									.build() );
				} )
				.isInstanceOf( AssertionError.class )
				.hasMessageContainingAll( "Name of current thread when executing work",
						"{ class=StubDocumentWork, type=ADD,",
						"Expecting", "wrongName", "to match pattern", "matching.*" );

		// Just tell backendMock to not fail this test:
		// we just had the failure we were looking for, so we're fine.
		backendMock.resetExpectations();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4308")
	void syncDuplicateIndexingWork() {
		backendMock.indexingWorkExpectations( BackendIndexingWorkExpectations.sync() );
		backendMock.expectWorks( INDEX_NAME ).add( b -> {} );

		StubDocumentWork indexingWork = StubDocumentWork.builder( StubDocumentWork.Type.ADD )
				.commit( DocumentCommitStrategy.FORCE )
				.refresh( DocumentRefreshStrategy.NONE )
				.build();

		assertThatCode( () -> backendMock.backendBehavior().createDocumentWork( INDEX_NAME, indexingWork ) )
				.doesNotThrowAnyException();
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork( INDEX_NAME, indexingWork ) )
				.doesNotThrowAnyException();

		// Duplicate execution => assertion error
		assertThatCode( () -> backendMock.backendBehavior().createDocumentWork( INDEX_NAME, indexingWork ) )
				.isInstanceOf( AssertionError.class );
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork( INDEX_NAME, indexingWork ) )
				.isInstanceOf( AssertionError.class );

		backendMock.resetExpectations();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4308")
	void asyncDuplicateIndexingWork() {
		backendMock.indexingWorkExpectations( BackendIndexingWorkExpectations.async( ".*" ) );
		backendMock.expectWorks( INDEX_NAME ).add( b -> {} );

		StubDocumentWork indexingWork = StubDocumentWork.builder( StubDocumentWork.Type.ADD )
				.commit( DocumentCommitStrategy.FORCE )
				.refresh( DocumentRefreshStrategy.NONE )
				.build();

		assertThatCode( () -> backendMock.backendBehavior().createDocumentWork( INDEX_NAME, indexingWork ) )
				.doesNotThrowAnyException();
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork( INDEX_NAME, indexingWork ) )
				.doesNotThrowAnyException();

		// Duplicate execution => works fine
		assertThatCode( () -> backendMock.backendBehavior().createDocumentWork( INDEX_NAME, indexingWork ) )
				.doesNotThrowAnyException();
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork( INDEX_NAME, indexingWork ) )
				.doesNotThrowAnyException();

		// Different, non-matching work => assertion error
		StubDocumentWork differentIndexingWork = StubDocumentWork.builder( StubDocumentWork.Type.ADD )
				.commit( DocumentCommitStrategy.NONE )
				.refresh( DocumentRefreshStrategy.NONE )
				.build();
		assertThatCode( () -> backendMock.backendBehavior().createDocumentWork( INDEX_NAME, differentIndexingWork ) )
				.isInstanceOf( AssertionError.class );
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork( INDEX_NAME, differentIndexingWork ) )
				.isInstanceOf( AssertionError.class );

		// New matched work
		backendMock.expectWorks( INDEX_NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE ).add( b -> {} );
		assertThatCode( () -> backendMock.backendBehavior().createDocumentWork( INDEX_NAME, differentIndexingWork ) )
				.doesNotThrowAnyException();
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork( INDEX_NAME, differentIndexingWork ) )
				.doesNotThrowAnyException();

		// The first matched work doesn't match anymore:
		// it's not considered "duplicate" if there was another matching work in the meantime.
		assertThatCode( () -> backendMock.backendBehavior().createDocumentWork( INDEX_NAME, indexingWork ) )
				.isInstanceOf( AssertionError.class );
		assertThatCode( () -> backendMock.backendBehavior().executeDocumentWork( INDEX_NAME, indexingWork ) )
				.isInstanceOf( AssertionError.class );

		backendMock.resetExpectations();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4308")
	void syncDuplicateNonIndexingWork() {
		backendMock.indexingWorkExpectations( BackendIndexingWorkExpectations.sync() );
		backendMock.expectIndexScaleWorks( INDEX_NAME ).flush();

		StubIndexScaleWork flushWork = StubIndexScaleWork.builder( StubIndexScaleWork.Type.FLUSH ).build();

		assertThatCode( () -> backendMock.backendBehavior().executeIndexScaleWork( INDEX_NAME, flushWork ) )
				.doesNotThrowAnyException();

		// Duplicate execution => assertion error
		assertThatCode( () -> backendMock.backendBehavior().executeIndexScaleWork( INDEX_NAME, flushWork ) )
				.isInstanceOf( AssertionError.class );

		backendMock.resetExpectations();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4308")
	void asyncDuplicateNonIndexingWork() {
		backendMock.indexingWorkExpectations( BackendIndexingWorkExpectations.async( ".*" ) );
		backendMock.expectIndexScaleWorks( INDEX_NAME ).flush();

		StubIndexScaleWork flushWork = StubIndexScaleWork.builder( StubIndexScaleWork.Type.FLUSH ).build();

		assertThatCode( () -> backendMock.backendBehavior().executeIndexScaleWork( INDEX_NAME, flushWork ) )
				.doesNotThrowAnyException();

		// Duplicate execution => assertion error
		assertThatCode( () -> backendMock.backendBehavior().executeIndexScaleWork( INDEX_NAME, flushWork ) )
				.isInstanceOf( AssertionError.class );

		backendMock.resetExpectations();
	}

	private AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownByCodeRunningInThreadWithName(
			String threadName, Runnable runnable) {
		CompletableFuture<?> future = new CompletableFuture<>();
		Thread thread = new Thread(
				() -> {
					try {
						runnable.run();
						future.complete( null );
					}
					catch (Throwable t) {
						future.completeExceptionally( t );
					}
				},
				threadName
		);
		thread.start();
		await().untilAsserted( () -> assertThat( future ).isDone() );
		return assertThat( Futures.getThrowableNow( future ) );
	}

}
