/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWorkExecutionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class LuceneWriteWorkProcessorTest extends EasyMockSupport {

	private static final String INDEX_NAME = "SomeIndexName";

	private EventContext indexEventContext = EventContexts.fromIndexName( INDEX_NAME );
	private IndexWriterDelegator indexWriterDelegatorMock = createStrictMock( IndexWriterDelegator.class );
	private FailureHandler failureHandlerMock = createStrictMock( FailureHandler.class );

	private LuceneWriteWorkProcessor processor = new LuceneWriteWorkProcessor(
			indexEventContext, indexWriterDelegatorMock,
			failureHandlerMock
	);

	private List<LuceneWriteWork<?>> workMocks = new ArrayList<>();

	@Test
	public void simple() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 4, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, true );
		testSuccessfulWorkSet( 2, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, true );
		testSuccessfulWorkSet( 1, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );

		resetAll();
		// There was no commit in the last workset, there must be one here
		indexWriterDelegatorMock.commit();
		replayAll();
		processor.endBatch();
		verifyAll();

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 4, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, true );
		testSuccessfulWorkSet( 2, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, true );
		testSuccessfulWorkSet( 1, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true );

		resetAll();
		// The last workset triggered a commit: no need for a commit here
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	@Test
	public void error_workExecute_commitNone() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a successful, committed workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Execute a workset where one work fails
		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException workException = new RuntimeException( "Some message" );
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		testWorkSetBeginning( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
		testFailingWork( workException );
		testSkippedWorks( 5 );
		resetAll();
		indexWriterDelegatorMock.forceLockRelease();
		expectWorkGetInfo( 2, 2 + 3 + 1 + 5 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		processor.afterWorkSet( workSetFuture, new Object() );
		verifyAll();

		IndexFailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( workException );
		assertThat( failureContext.getFailingOperation() )
				.isEqualTo( workInfo( 7 ) );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// Works from the previous, uncommitted workset
						workInfo( 2 ), workInfo( 3 ),
						// Works from the current workset
						workInfo( 4 ), workInfo( 5 ), workInfo( 6 ), workInfo( 7 ),
						workInfo( 8 ), workInfo( 9 ), workInfo( 10 ), workInfo( 11 ),
						workInfo( 12 )
				);
		FutureAssert.assertThat( workSetFuture ).isFailed( workException );

		// Subsequent worksets must be executed regardless of previous failures in the same batch
		testSuccessfulWorkSet(
				3,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// A work may have failed, but there were still successful changes after the failure: these must be committed
		resetAll();
		indexWriterDelegatorMock.commit();
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	/**
	 * Test that there is no workset commit after a failure, even if the commit strategy is FORCE.
	 */
	@Test
	public void error_workExecute_commitForce() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a successful, committed workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Execute a workset where one work fails
		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException workException = new RuntimeException( "Some message" );
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		testWorkSetBeginning( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
		testFailingWork( workException );
		testSkippedWorks( 5 );
		resetAll();
		indexWriterDelegatorMock.forceLockRelease();
		expectWorkGetInfo( 2, 2 + 3 + 1 + 5 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		processor.afterWorkSet( workSetFuture, new Object() );
		verifyAll();

		IndexFailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( workException );
		assertThat( failureContext.getFailingOperation() )
				.isEqualTo( workInfo( 7 ) );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// Works from the previous, uncommitted workset
						workInfo( 2 ), workInfo( 3 ),
						// Works from the current workset
						workInfo( 4 ), workInfo( 5 ), workInfo( 6 ),
						workInfo( 7 ),
						workInfo( 8 ), workInfo( 9 ), workInfo( 10 ), workInfo( 11 ), workInfo( 12 )
				);
		FutureAssert.assertThat( workSetFuture ).isFailed( workException );

		// Subsequent worksets must be executed regardless of previous failures in the same batch
		testSuccessfulWorkSet(
				3,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// A work may have failed, but there still were successful changes after the failure: these must be committed
		resetAll();
		indexWriterDelegatorMock.commit();
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	@Test
	public void error_workExecuteAndForceLockRelease() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a successful, committed workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Execute a workset where one work fails
		RuntimeException workException = new RuntimeException( "Some message" );
		testWorkSetBeginning( 2, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
		testFailingWork( workException );
		testSkippedWorks( 4 );

		// When ending the workset, forceLockRelease fails...
		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException forceLockReleaseException = new RuntimeException( "Some other message" );
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		Object workSetResult = new Object();
		resetAll();
		indexWriterDelegatorMock.forceLockRelease();
		expectLastCall().andThrow( forceLockReleaseException );
		expectWorkGetInfo( 2, 2 + 2 + 1 + 4 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		processor.afterWorkSet( workSetFuture, workSetResult );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isFailed( workException );

		IndexFailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( workException );
		assertThat( failureContext.getFailingOperation() )
				.isEqualTo( workInfo( 6 ) );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// Works from the previous, uncommitted workset
						workInfo( 2 ), workInfo( 3 ),
						// Works from the current workset
						workInfo( 4 ), workInfo( 5 ), workInfo( 6 ), workInfo( 7 ),
						workInfo( 8 ), workInfo( 9 ), workInfo( 10 )
				);

		assertThat( failureContext.getThrowable().getSuppressed() )
				.hasSize( 1 )
				.satisfies(
						suppressed -> assertThat( suppressed[0] )
								.hasMessageContaining( "Unable to clean up" )
								.hasMessageContaining( INDEX_NAME )
								.hasCause( forceLockReleaseException )
				);

		// Subsequent worksets must be executed regardless of previous failures in the same batch
		testSuccessfulWorkSet(
				3,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// A work may have failed, but there were still successful changes, before and after the failure: these must be committed
		resetAll();
		indexWriterDelegatorMock.commit();
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	@Test
	public void error_workSetCommit() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a successful, committed workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Start a new workset
		testWorkSetBeginning(
				6,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);

		// Fail upon workset commit

		RuntimeException commitException = new RuntimeException( "Some message" );

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		Object workSetResult = new Object();
		resetAll();
		indexWriterDelegatorMock.commit();
		expectLastCall().andThrow( commitException );
		indexWriterDelegatorMock.forceLockRelease();
		expectWorkGetInfo( 2, 2 + 6 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		// We don't expect any commit when a workset fails
		replayAll();
		processor.afterWorkSet( workSetFuture, workSetResult );
		verifyAll();

		IndexFailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to commit" )
				.hasMessageContaining( INDEX_NAME )
				.hasCause( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a set of index works" );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// Works from the previous, uncommitted workset
						workInfo( 2 ), workInfo( 3 ),
						// Works from the current workset
						workInfo( 4 ), workInfo( 5 ), workInfo( 6 ),
						workInfo( 7 ), workInfo( 8 ), workInfo( 9 )
				);

		FutureAssert.assertThat( workSetFuture ).isFailed( failureContext.getThrowable() );

		resetAll();
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	@Test
	public void error_workSetCommitAndForceLockRelease() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a successful, committed workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Start a new workset
		testWorkSetBeginning(
				6,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);

		// Fail upon workset commit AND forceLockRelease...

		RuntimeException commitException = new RuntimeException( "Some message" );
		RuntimeException forceLockReleaseException = new RuntimeException( "Some other message" );

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		Object workSetResult = new Object();
		resetAll();
		indexWriterDelegatorMock.commit();
		expectLastCall().andThrow( commitException );
		indexWriterDelegatorMock.forceLockRelease();
		expectLastCall().andThrow( forceLockReleaseException );
		expectWorkGetInfo( 2, 2 + 6 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		// We don't expect any commit when a workset fails
		replayAll();
		processor.afterWorkSet( workSetFuture, workSetResult );
		verifyAll();

		IndexFailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to commit" )
				.hasMessageContaining( INDEX_NAME )
				.hasCause( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a set of index works" );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// Works from the previous, uncommitted workset
						workInfo( 2 ), workInfo( 3 ),
						// Works from the current workset
						workInfo( 4 ), workInfo( 5 ), workInfo( 6 ),
						workInfo( 7 ), workInfo( 8 ), workInfo( 9 )
				);

		assertThat( failureContext.getThrowable().getSuppressed() )
				.hasSize( 1 )
				.satisfies(
						suppressed -> assertThat( suppressed[0] )
								.hasMessageContaining( "Unable to clean up" )
								.hasMessageContaining( INDEX_NAME )
								.hasCause( forceLockReleaseException )
				);

		FutureAssert.assertThat( workSetFuture ).isFailed( failureContext.getThrowable() );

		resetAll();
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	@Test
	public void error_batchCommit() throws IOException {
		RuntimeException commitException = new RuntimeException( "Some message" );

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute worksets
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true
		);
		testSuccessfulWorkSet(
				4,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);
		testSuccessfulWorkSet(
				6,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Fail upon batch commit

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		indexWriterDelegatorMock.commit();
		expectLastCall().andThrow( commitException );
		indexWriterDelegatorMock.forceLockRelease();
		expectWorkGetInfo( 2, 4 + 6 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		processor.endBatch();
		verifyAll();

		IndexFailureContext failureContext = failureContextCapture.getValue();

		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to commit" )
				.hasMessageContaining( INDEX_NAME )
				.hasCause( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a batch of index works" );
		// Uncommitted operations must include works from all previous works since the last commit
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// First uncommitted workset
						workInfo( 2 ), workInfo( 3 ), workInfo( 4 ), workInfo( 5 ),
						// Second uncommitted workset
						workInfo( 6 ), workInfo( 7 ), workInfo( 8 ), workInfo( 9 ),
						workInfo( 10 ), workInfo( 11 )
				);
	}

	@Test
	public void error_batchCommitAndForceLockRelease() throws IOException {
		RuntimeException commitException = new RuntimeException( "Some message" );
		RuntimeException forceLockReleaseException = new RuntimeException( "Some other message" );

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute worksets
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true
		);
		testSuccessfulWorkSet(
				4,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);
		testSuccessfulWorkSet(
				6,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Fail upon batch commit AND forceLockRelease...

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		indexWriterDelegatorMock.commit();
		expectLastCall().andThrow( commitException );
		indexWriterDelegatorMock.forceLockRelease();
		expectLastCall().andThrow( forceLockReleaseException );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		expectWorkGetInfo( 2, 4 + 6 );
		replayAll();
		processor.endBatch();
		verifyAll();

		IndexFailureContext failureContext = failureContextCapture.getValue();

		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to commit" )
				.hasMessageContaining( INDEX_NAME )
				.hasCause( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a batch of index works" );
		// Uncommitted operations must include works from all previous works since the last commit
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// First uncommitted workset
						workInfo( 2 ), workInfo( 3 ), workInfo( 4 ), workInfo( 5 ),
						// Second uncommitted workset
						workInfo( 6 ), workInfo( 7 ), workInfo( 8 ), workInfo( 9 ),
						workInfo( 10 ), workInfo( 11 )
				);

		assertThat( failureContext.getThrowable().getSuppressed() )
				.hasSize( 1 )
				.satisfies(
						suppressed -> assertThat( suppressed[0] )
								.hasMessageContaining( "Unable to clean up" )
								.hasMessageContaining( INDEX_NAME )
								.hasCause( forceLockReleaseException )
				);

		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a batch of index works" );
	}

	private void testSuccessfulWorkSet(int workCount,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			boolean expectCommit) throws IOException {
		testWorkSetBeginning( workCount, commitStrategy, refreshStrategy );

		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		Object workSetResult = new Object();
		resetAll();
		if ( expectCommit ) {
			indexWriterDelegatorMock.commit();
		}
		replayAll();
		processor.afterWorkSet( workSetFuture, workSetResult );
		verifyAll();
		FutureAssert.assertThat( workSetFuture ).isSuccessful( workSetResult );
	}

	private void testFailingWork(RuntimeException workException) throws IOException {
		Capture<LuceneWriteWorkExecutionContext> contextCapture = Capture.newInstance();

		LuceneWriteWork<Object> failingWork = createWorkMock();

		resetAll();
		expect( failingWork.execute( capture( contextCapture ) ) ).andThrow( workException );
		expect( failingWork.getInfo() ).andReturn( workInfo( failingWork ) );
		replayAll();
		assertThat( processor.submit( failingWork ) ).isNull();
		verifyAll();
		testContext( contextCapture.getValue() );
	}

	private void testWorkSetBeginning(int workCount,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) throws IOException {
		Capture<LuceneWriteWorkExecutionContext> contextCapture = Capture.newInstance();

		resetAll();
		replayAll();
		processor.beforeWorkSet( commitStrategy, refreshStrategy );
		verifyAll();

		for ( int i = 0; i < workCount; ++i ) {
			LuceneWriteWork<Object> work = createWorkMock();
			Object workResult = new Object();

			resetAll();
			expect( work.execute( capture( contextCapture ) ) ).andReturn( workResult );
			replayAll();
			assertThat( processor.submit( work ) ).isEqualTo( workResult );
			verifyAll();

			testContext( contextCapture.getValue() );
		}
	}

	private void testSkippedWorks(int workCount) {
		for ( int i = 0; i < workCount; ++i ) {
			LuceneWriteWork<Object> work = createWorkMock();

			resetAll();
			replayAll();
			assertThat( processor.submit( work ) ).isNull();
			verifyAll();
		}
	}

	private void expectWorkGetInfo(int firstId, int workCount) {
		for ( int i = firstId; i < firstId + workCount; ++i ) {
			LuceneWriteWork<?> workMock = workMocks.get( i );
			EasyMock.expect( workMock.getInfo() ).andReturn( workInfo( i ) );
		}
	}

	private void testContext(LuceneWriteWorkExecutionContext context) throws IOException {
		resetAll();
		replayAll();
		assertThat( context.getIndexWriterDelegator() ).isSameAs( indexWriterDelegatorMock );
		verifyAll();
	}

	private <T> LuceneWriteWork<T> createWorkMock() {
		String workName = workInfo( workMocks.size() );
		LuceneWriteWork<T> workMock = createStrictMock( workName, LuceneWriteWork.class );
		workMocks.add( workMock );
		return workMock;
	}

	private String workInfo(LuceneWriteWork<?> work) {
		return workInfo( workMocks.indexOf( work ) );
	}

	private String workInfo(int index) {
		return "work_" + index;
	}

}