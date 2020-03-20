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
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class LuceneWriteWorkProcessorTest extends EasyMockSupport {

	private static final String INDEX_NAME = "SomeIndexName";

	private EventContext indexEventContext = EventContexts.fromIndexName( INDEX_NAME );
	private IndexAccessor indexAccessorMock = createStrictMock( IndexAccessor.class );
	private IndexWriterDelegator indexWriterDelegatorMock = createStrictMock( IndexWriterDelegator.class );

	private LuceneWriteWorkProcessor processor = new LuceneWriteWorkProcessor(
			indexEventContext, indexAccessorMock
	);

	private List<WriteWork<?>> workMocks = new ArrayList<>();

	@Test
	public void batch() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWriteWorks( 200 );

		resetAll();
		// Give a chance to the I/O strategy to schedule a delayed commit.
		indexAccessorMock.commitOrDelay();
		replayAll();
		processor.endBatch();
		verifyAll();

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWriteWorks( 100 );

		resetAll();
		// Give a chance to the I/O strategy to schedule a delayed commit.
		indexAccessorMock.commitOrDelay();
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteWithNothingToCommit();
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
		RuntimeException workException = new RuntimeException( "Some message" );
		WriteWork<Object> failingWork = createWorkMock();
		resetAll();
		expect( failingWork.execute( EasyMock.anyObject() ) ).andThrow( workException );
		expectWorkGetInfo( 50 );
		indexAccessorMock.cleanUpAfterFailure( workException, workInfo( 50 ) );
		replayAll();
		SubTest.expectException( () -> processor.submit( failingWork ) )
				.assertThrown().isSameAs( workException );
		verifyAll();

		// Subsequent works must be executed regardless of previous failures in the same batch
		testSuccessfulWriteWorks( 10 );

		resetAll();
		indexAccessorMock.commitOrDelay();
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteWithNothingToCommit();
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
		RuntimeException commitException = new RuntimeException( "Some message" );
		resetAll();
		indexAccessorMock.commit();
		expectLastCall().andThrow( commitException );
		indexAccessorMock.cleanUpAfterFailure( commitException, "Commit after a set of index works" );
		replayAll();
		SubTest.expectException( () -> processor.forceCommit() )
				.assertThrown()
				.isSameAs( commitException );
		verifyAll();

		resetAll();
		indexAccessorMock.commitOrDelay();
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteWithNothingToCommit();
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
	public void error_batchCommit() throws IOException {
		RuntimeException commitException = new RuntimeException( "Some message" );

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a few successful works
		testSuccessfulWriteWorks( 200 );

		// Fail upon batch commit
		resetAll();
		indexAccessorMock.commitOrDelay();
		expectLastCall().andThrow( commitException );
		indexAccessorMock.cleanUpAfterFailure( commitException, "Commit after a batch of index works" );
		replayAll();
		processor.endBatch();
		verifyAll();

		checkCompleteWithNothingToCommit();
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

	private void checkCompleteWithNothingToCommit() {
		resetAll();
		// The index accessor (or the underlying writer) is responsible for detecting there is nothing to commit.
		indexAccessorMock.commitOrDelay();
		replayAll();
		processor.complete();
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

	private String workInfo(int index) {
		return "work_" + index;
	}

}