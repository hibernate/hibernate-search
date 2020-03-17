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

import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWorkExecutionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class LuceneWriteWorkProcessorTest extends EasyMockSupport {

	private static final String INDEX_NAME = "SomeIndexName";

	private EventContext indexEventContext = EventContexts.fromIndexName( INDEX_NAME );
	private IndexAccessor indexAccessorMock = createStrictMock( IndexAccessor.class );
	private IndexWriterDelegator indexWriterDelegatorMock = createStrictMock( IndexWriterDelegator.class );
	private FailureHandler failureHandlerMock = createStrictMock( FailureHandler.class );

	private LuceneWriteWorkProcessor processor = new LuceneWriteWorkProcessor(
			INDEX_NAME, indexEventContext,
			indexAccessorMock, failureHandlerMock
	);

	private List<LuceneWriteWork<?>> workMocks = new ArrayList<>();

	@Test
	public void immediateCommitStrategy() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 4, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, true, false );
		testSuccessfulWorkSet( 2, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, false, true );
		testSuccessfulWorkSet( 1, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true, true );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );

		resetAll();
		// There was no commit in the last workset, there must be one here
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 4, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, true, false );
		testSuccessfulWorkSet( 2, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, false, true );
		testSuccessfulWorkSet( 1, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true, true );

		resetAll();
		// The last workset triggered a commit: no need for a commit here
		// but we rely on the index accessor or index writer to detect that
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
	}

	@Test
	public void delayedCommitStrategy_endBatchCommitNotNecessary() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, false, true );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, true, false );

		resetAll();
		// The last workset triggered a commit: no need for a commit here
		// but we rely on the index accessor or index writer to detect that
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		// The executor does not have any additional work, so it calls completeOrDelay() just after endBatch().
		resetAll();
		// The last workset triggered a commit: no need for a commit here
		// but we rely on the index accessor or index writer to detect that
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 0L );
		verifyAll();
	}

	@Test
	public void delayedCommitStrategy_endBatchCommitNecessary_noAdditionalWorkDuringDelay() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true, true );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );

		resetAll();
		// There was no commit in the last workset, there must be one here
		expect( indexAccessorMock.commitOrDelay() )
				// The I/O strategy decides that it's too early for a commit.
				.andReturn( 1000L );
		replayAll();
		processor.endBatch();
		verifyAll();

		// The executor does not have any additional work, so it calls completeOrDelay() just after endBatch().
		resetAll();
		// There was no commit in the last batch, there must be one here
		expect( indexAccessorMock.commitOrDelay() )
				// Almost no time passed since the call to endBatch(),
				// so the I/O strategy decides that it's still too early for a commit.
				.andReturn( 999L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 999L );
		verifyAll();

		// 999 ms pass...

		// The executor didn't receive any additional work, so it calls completeOrDelay() again some time later.
		resetAll();
		// There was no commit in the last batch, there must be one here
		expect( indexAccessorMock.commitOrDelay() )
				// The I/O strategy decides that it's now time for a commit.
				.andReturn( 0L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 0L );
		verifyAll();
	}

	@Test
	public void delayedCommitStrategy_endBatchCommitNecessary_someAdditionalWorkDuringDelay_endBatchCommitNotNecessary() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true, true );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );

		resetAll();
		// There was no commit in the last workset, there must be one here
		expect( indexAccessorMock.commitOrDelay() )
				// The I/O strategy decides that it's too early for a commit.
				.andReturn( 1000L );
		replayAll();
		processor.endBatch();
		verifyAll();

		// The executor does not have any additional work, so it calls completeOrDelay() just after endBatch().
		resetAll();
		// There was no commit in the last batch, there must be one here
		expect( indexAccessorMock.commitOrDelay() )
				// Almost no time passed since the call to endBatch(),
				// so the I/O strategy decides that it's still too early for a commit.
				.andReturn( 999L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 999L );
		verifyAll();

		// 500 ms pass...

		// Some work is submitted to the executor!
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, true, false );
		resetAll();
		// The last workset triggered a commit: no need for a commit here
		// but we rely on the index accessor or index writer to detect that
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		// The executor does not have any additional work, so it calls completeOrDelay() just after endBatch().
		resetAll();
		// The last workset triggered a commit: no need for a commit here
		// but we rely on the index accessor or index writer to detect that
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 0L );
		verifyAll();
	}

	@Test
	public void delayedCommitStrategy_endBatchCommitNecessary_someAdditionalWorkDuringDelay_endBatchCommitNecessary() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true, true );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );

		resetAll();
		expect( indexAccessorMock.commitOrDelay() )
				// The I/O strategy decides that it's too early for a commit.
				.andReturn( 1000L );
		replayAll();
		processor.endBatch();
		verifyAll();

		// The executor does not have any additional work, so it calls completeOrDelay() just after endBatch().
		resetAll();
		expect( indexAccessorMock.commitOrDelay() )
				// Almost no time passed since the call to endBatch(),
				// so the I/O strategy decides that it's still too early for a commit.
				.andReturn( 999L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 999L );
		verifyAll();

		// 500 ms pass...

		// Some work is submitted to the executor!
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false, false );
		resetAll();
		expect( indexAccessorMock.commitOrDelay() )
				// The I/O strategy decides that it's too early for a commit.
				.andReturn( 499L );
		replayAll();
		processor.endBatch();
		verifyAll();

		// The executor does not have any additional work, so it calls completeOrDelay() just after endBatch().
		resetAll();
		expect( indexAccessorMock.commitOrDelay() )
				// Almost no time passed since the call to endBatch(),
				// so the I/O strategy decides that it's still too early for a commit.
				.andReturn( 498L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 498L );
		verifyAll();

		// 498 ms pass...

		// The executor didn't receive any additional work, so it calls completeOrDelay() again some time later.
		resetAll();
		expect( indexAccessorMock.commitOrDelay() )
				// The I/O strategy decides that it's now time for a commit.
				.andReturn( 0L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 0L );
		verifyAll();
	}

	@Test
	public void error_workExecute_commitNone_refreshNone() throws IOException {
		doTestErrorWorkExecute( DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
	}

	@Test
	public void error_workExecute_commitNone_refreshForce() throws IOException {
		doTestErrorWorkExecute( DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE );
	}

	@Test
	public void error_workExecute_commitForce_refreshNone() throws IOException {
		doTestErrorWorkExecute( DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
	}

	@Test
	public void error_workExecute_commitForce_refreshForce() throws IOException {
		doTestErrorWorkExecute( DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE );
	}

	/**
	 * Test that there is no workset commit/refresh after a failure,
	 * regardless of the commit/refresh strategy passed in parameter.
	 */
	private void doTestErrorWorkExecute(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a successful, committed workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true,
				false
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);

		// Start a workset with a few successful works
		testWorkSetBeginning( 3, commitStrategy, refreshStrategy );

		// ... and suddenly a failing work
		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException workException = new RuntimeException( "Some message" );
		LuceneWriteWork<Object> failingWork = createWorkMock();
		resetAll();
		expect( failingWork.execute( EasyMock.anyObject() ) ).andThrow( workException );
		indexAccessorMock.reset();
		expectWorkGetInfo( 7 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		SubTest.expectException( () -> processor.submit( failingWork ) )
				.assertThrown().isSameAs( workException );
		verifyAll();

		// Note that callers are not supposed to call any method on the processor after a failure in a workset

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A failure occurred during a low-level write operation on index '" + INDEX_NAME + "'",
						"Some write operations may have been lost as a result"
				)
				.hasCause( workException );
		assertThat( failureContext.getFailingOperation() )
				.isEqualTo( workInfo( 7 ) );

		// Subsequent worksets must be executed regardless of previous failures in the same batch
		testSuccessfulWorkSet(
				3,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
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
				true,
				false
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);

		// Start a workset with a few successful works
		testWorkSetBeginning( 2, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );

		// ... and suddenly a failing work
		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException workException = new RuntimeException( "Some message" );
		LuceneWriteWork<Object> failingWork = createWorkMock();
		RuntimeException forceLockReleaseException = new RuntimeException( "Some other message" );
		resetAll();
		expect( failingWork.execute( EasyMock.anyObject() ) ).andThrow( workException );
		// ... and forceLockRelease fails too
		indexAccessorMock.reset();
		expectLastCall().andThrow( forceLockReleaseException );
		expectWorkGetInfo( 6 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		SubTest.expectException( () -> processor.submit( failingWork ) )
				.assertThrown().isSameAs( workException );
		verifyAll();

		// Note that callers are not supposed to call any method on the processor after a failure in a workset

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A failure occurred during a low-level write operation on index '" + INDEX_NAME + "'",
						"Some write operations may have been lost as a result"
				)
				.hasCause( workException );
		assertThat( failureContext.getFailingOperation() )
				.isEqualTo( workInfo( 6 ) );

		assertThat( failureContext.getThrowable().getCause().getSuppressed() )
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
				false,
				false
		);

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
	}

	@Test
	public void error_workSetCommit_refreshNone() throws IOException {
		doTestErrorWorkSetCommit( DocumentRefreshStrategy.NONE );
	}

	/**
	 * Test that the refresh strategy is ignored when an error occurs during the workset commit.
	 */
	@Test
	public void error_workSetCommit_refreshForce() throws IOException {
		doTestErrorWorkSetCommit( DocumentRefreshStrategy.FORCE );
	}

	private void doTestErrorWorkSetCommit(DocumentRefreshStrategy refreshStrategy) throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a successful, committed workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true,
				false
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);

		// Start a workset with a few successful works
		testWorkSetBeginning(
				6,
				DocumentCommitStrategy.FORCE, refreshStrategy
		);

		// ... but fail upon workset commit
		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException commitException = new RuntimeException( "Some message" );
		resetAll();
		indexAccessorMock.commit();
		expectLastCall().andThrow( commitException );
		indexAccessorMock.reset();
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		SubTest.expectException( () -> processor.afterSuccessfulWorkSet() )
				.assertThrown()
				.isSameAs( commitException );
		verifyAll();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A failure occurred during a low-level write operation on index '" + INDEX_NAME + "'",
						"Some write operations may have been lost as a result"
				)
				.hasCause( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a set of index works" );

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
	}

	@Test
	public void error_workSetRefresh_commitNone() throws IOException {
		doTestErrorWorkSetRefresh( DocumentCommitStrategy.NONE, false );
	}

	@Test
	public void error_workSetRefresh_commitForce() throws IOException {
		doTestErrorWorkSetRefresh( DocumentCommitStrategy.FORCE, true );
	}

	private void doTestErrorWorkSetRefresh(DocumentCommitStrategy commitStrategy, boolean expectWorkSetCommit) throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a successful, committed workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE,
				true,
				false
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);

		// Start a workset with a few successful works
		testWorkSetBeginning(
				6,
				commitStrategy, DocumentRefreshStrategy.FORCE
		);

		// ... but fail upon workset refresh
		RuntimeException refreshException = new RuntimeException( "Some message" );
		resetAll();
		if ( expectWorkSetCommit ) {
			indexAccessorMock.commit();
		}
		indexAccessorMock.refresh();
		expectLastCall().andThrow( refreshException );
		replayAll();
		SubTest.expectException( () -> processor.afterSuccessfulWorkSet() )
				.assertThrown()
				.isSameAs( refreshException );
		verifyAll();

		resetAll();
		// If there a commit in the last workset, we don't really need one here
		// but we rely on the index accessor or index writer to detect that
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
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
				true,
				false
		);

		// Execute a successful, uncommitted workset
		testSuccessfulWorkSet(
				2,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);

		// Start a new workset
		testWorkSetBeginning(
				6,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);

		// ... but fail upon workset commit
		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException commitException = new RuntimeException( "Some message" );
		RuntimeException forceLockReleaseException = new RuntimeException( "Some other message" );
		resetAll();
		indexAccessorMock.commit();
		expectLastCall().andThrow( commitException );
		// ... and forceLockRelease fails too
		indexAccessorMock.reset();
		expectLastCall().andThrow( forceLockReleaseException );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		// We don't expect any commit when a workset fails
		replayAll();
		SubTest.expectException( () -> processor.afterSuccessfulWorkSet() )
				.assertThrown()
				.isSameAs( commitException );
		verifyAll();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A failure occurred during a low-level write operation on index '" + INDEX_NAME + "'",
						"Some write operations may have been lost as a result"
				)
				.hasCause( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a set of index works" );

		assertThat( failureContext.getThrowable().getCause().getSuppressed() )
				.hasSize( 1 )
				.satisfies(
						suppressed -> assertThat( suppressed[0] )
								.hasMessageContaining( "Unable to clean up" )
								.hasMessageContaining( INDEX_NAME )
								.hasCause( forceLockReleaseException )
				);

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
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
				true,
				false
		);
		testSuccessfulWorkSet(
				4,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);
		testSuccessfulWorkSet(
				6,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);

		// Fail upon batch commit

		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andThrow( commitException );
		indexAccessorMock.reset();
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		processor.endBatch();
		verifyAll();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A failure occurred during a low-level write operation on index '" + INDEX_NAME + "'",
						"Some write operations may have been lost as a result"
				)
				.hasCause( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a batch of index works" );

		checkCompleteOrDelayWithNothingToCommit();
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
				true,
				false
		);
		testSuccessfulWorkSet(
				4,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);
		testSuccessfulWorkSet(
				6,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false,
				false
		);

		// Fail upon batch commit AND forceLockRelease...

		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andThrow( commitException );
		indexAccessorMock.reset();
		expectLastCall().andThrow( forceLockReleaseException );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		processor.endBatch();
		verifyAll();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A failure occurred during a low-level write operation on index '" + INDEX_NAME + "'",
						"Some write operations may have been lost as a result"
				)
				.hasCause( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a batch of index works" );

		assertThat( failureContext.getThrowable().getCause().getSuppressed() )
				.hasSize( 1 )
				.satisfies(
						suppressed -> assertThat( suppressed[0] )
								.hasMessageContaining( "Unable to clean up" )
								.hasMessageContaining( INDEX_NAME )
								.hasCause( forceLockReleaseException )
				);

		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a batch of index works" );

		checkCompleteOrDelayWithNothingToCommit();
	}

	private void testSuccessfulWorkSet(int workCount,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			boolean expectCommit, boolean expectRefresh) throws IOException {
		testWorkSetBeginning( workCount, commitStrategy, refreshStrategy );

		resetAll();
		if ( expectCommit ) {
			indexAccessorMock.commit();
		}
		if ( expectRefresh ) {
			indexAccessorMock.refresh();
		}
		replayAll();
		processor.afterSuccessfulWorkSet();
		verifyAll();
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

	private void checkCompleteOrDelayWithNothingToCommit() {
		resetAll();
		// The index accessor (or the underlying writer) is responsible for detecting there is nothing to commit.
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		Assertions.assertThat( processor.completeOrDelay() ).isEqualTo( 0L );
		verifyAll();
	}

	private void expectWorkGetInfo(int ... ids) {
		for ( int id : ids ) {
			LuceneWriteWork<?> workMock = workMocks.get( id );
			EasyMock.expect( workMock.getInfo() ).andReturn( workInfo( id ) );
		}
	}

	private void testContext(LuceneWriteWorkExecutionContext context) throws IOException {
		resetAll();
		replayAll();
		assertThat( context.getEventContext() ).isSameAs( indexEventContext );
		verifyAll();

		resetAll();
		expect( indexAccessorMock.getIndexWriterDelegator() ).andReturn( indexWriterDelegatorMock );
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