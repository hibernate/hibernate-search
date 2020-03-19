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
import org.hibernate.search.backend.lucene.work.impl.WriteWork;
import org.hibernate.search.backend.lucene.work.impl.WriteWorkExecutionContext;
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

	private List<WriteWork<?>> workMocks = new ArrayList<>();

	@Test
	public void immediateCommitStrategy() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWriteWorks( 200 );

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWriteWorks( 100 );

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
	}

	@Test
	public void delayedCommitStrategy_noDelay() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWriteWorks( 200 );

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		// The executor does not have any additional work, so it calls completeOrDelay() just after endBatch().
		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		assertThat( processor.completeOrDelay() ).isEqualTo( 0L );
		verifyAll();
	}

	@Test
	public void delayedCommitStrategy_delay_noAdditionalWorkDuringDelay() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWriteWorks( 200 );

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

		// 999 ms pass...

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
	public void delayedCommitStrategy_delay_someAdditionalWorkDuringDelay() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a few successful works
		testSuccessfulWriteWorks( 50 );

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
		testSuccessfulWriteWorks( 10 );
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
	public void error_workExecute() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a few successful works
		testSuccessfulWriteWorks( 50 );

		// ... and suddenly a failing work
		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException workException = new RuntimeException( "Some message" );
		WriteWork<Object> failingWork = createWorkMock();
		resetAll();
		expect( failingWork.execute( EasyMock.anyObject() ) ).andThrow( workException );
		indexAccessorMock.reset();
		expectWorkGetInfo( 50 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		SubTest.expectException( () -> processor.submit( failingWork ) )
				.assertThrown().isSameAs( workException );
		verifyAll();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A failure occurred during a low-level write operation on index '" + INDEX_NAME + "'",
						"Some write operations may have been lost as a result"
				)
				.hasCause( workException );
		assertThat( failureContext.getFailingOperation() )
				.isEqualTo( workInfo( 50 ) );

		// Subsequent works must be executed regardless of previous failures in the same batch
		testSuccessfulWriteWorks( 10 );

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
	}

	@Test
	public void error_workExecuteAndReset() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a few successful works
		testSuccessfulWriteWorks( 50 );

		// ... and suddenly a failing work
		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException workException = new RuntimeException( "Some message" );
		WriteWork<Object> failingWork = createWorkMock();
		RuntimeException resetException = new RuntimeException( "Some other message" );
		resetAll();
		expect( failingWork.execute( EasyMock.anyObject() ) ).andThrow( workException );
		// ... and reset fails too
		indexAccessorMock.reset();
		expectLastCall().andThrow( resetException );
		expectWorkGetInfo( 50 );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		SubTest.expectException( () -> processor.submit( failingWork ) )
				.assertThrown().isSameAs( workException );
		verifyAll();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A failure occurred during a low-level write operation on index '" + INDEX_NAME + "'",
						"Some write operations may have been lost as a result"
				)
				.hasCause( workException );
		assertThat( failureContext.getFailingOperation() )
				.isEqualTo( workInfo( 50 ) );

		assertThat( failureContext.getThrowable().getCause().getSuppressed() )
				.hasSize( 1 )
				.satisfies(
						suppressed -> assertThat( suppressed[0] )
								.hasMessageContaining( "Unable to clean up" )
								.hasMessageContaining( INDEX_NAME )
								.hasCause( resetException )
				);

		// Subsequent works must be executed regardless of previous failures in the same batch
		testSuccessfulWriteWorks( 20 );

		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteOrDelayWithNothingToCommit();
	}

	@Test
	public void forceCommit() {
		resetAll();
		indexAccessorMock.commit();
		replayAll();
		processor.forceCommit();
		verifyAll();
	}

	@Test
	public void error_forceCommit() throws IOException {
		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException commitException = new RuntimeException( "Some message" );
		resetAll();
		indexAccessorMock.commit();
		expectLastCall().andThrow( commitException );
		indexAccessorMock.reset();
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		SubTest.expectException( () -> processor.forceCommit() )
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
	public void forceRefresh() {
		resetAll();
		indexAccessorMock.refresh();
		replayAll();
		processor.forceRefresh();
		verifyAll();
	}

	@Test
	public void error_forceRefresh() {
		RuntimeException refreshException = new RuntimeException( "Some message" );
		resetAll();
		indexAccessorMock.refresh();
		expectLastCall().andThrow( refreshException );
		replayAll();
		SubTest.expectException( () -> processor.forceRefresh() )
				.assertThrown()
				.isSameAs( refreshException );
		verifyAll();
	}

	@Test
	public void error_forceCommitAndReset() throws IOException {
		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException commitException = new RuntimeException( "Some message" );
		RuntimeException resetException = new RuntimeException( "Some other message" );
		resetAll();
		// The commit fails...
		indexAccessorMock.commit();
		expectLastCall().andThrow( commitException );
		// ... and reset fails too
		indexAccessorMock.reset();
		expectLastCall().andThrow( resetException );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		SubTest.expectException( () -> processor.forceCommit() )
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
								.hasCause( resetException )
				);
	}

	@Test
	public void error_batchCommit() throws IOException {
		RuntimeException commitException = new RuntimeException( "Some message" );

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a few successful works
		testSuccessfulWriteWorks( 200 );

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
	public void error_batchCommitAndReset() throws IOException {
		RuntimeException commitException = new RuntimeException( "Some message" );
		RuntimeException resetException = new RuntimeException( "Some other message" );

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a few successful works
		testSuccessfulWriteWorks( 200 );

		// Fail upon batch commit AND reset...

		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		expect( indexAccessorMock.commitOrDelay() ).andThrow( commitException );
		indexAccessorMock.reset();
		expectLastCall().andThrow( resetException );
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
								.hasCause( resetException )
				);

		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Commit after a batch of index works" );

		checkCompleteOrDelayWithNothingToCommit();
	}

	private void testSuccessfulWriteWorks(int workCount) throws IOException {
		Capture<WriteWorkExecutionContext> contextCapture = Capture.newInstance();

		for ( int i = 0; i < workCount; ++i ) {
			WriteWork<Object> work = createWorkMock();
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
			WriteWork<?> workMock = workMocks.get( id );
			EasyMock.expect( workMock.getInfo() ).andReturn( workInfo( id ) );
		}
	}

	private void testContext(WriteWorkExecutionContext context) throws IOException {
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

	private <T> WriteWork<T> createWorkMock() {
		String workName = workInfo( workMocks.size() );
		WriteWork<T> workMock = createStrictMock( workName, WriteWork.class );
		workMocks.add( workMock );
		return workMock;
	}

	private String workInfo(WriteWork<?> work) {
		return workInfo( workMocks.indexOf( work ) );
	}

	private String workInfo(int index) {
		return "work_" + index;
	}

}