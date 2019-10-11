/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkProcessor;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

/**
 * Test worksets produced by indexing plans.
 */
public class LuceneIndexingPlanWriteWorkSetTest extends EasyMockSupport {

	private LuceneWriteWorkProcessor processorMock = createStrictMock( LuceneWriteWorkProcessor.class );
	private FailureHandler failureHandlerMock = createStrictMock( FailureHandler.class );

	private List<LuceneWriteWork<?>> workMocks = new ArrayList<>();

	@Test
	public void success_commitNone_refreshNone() {
		doTestSuccess( DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
	}

	@Test
	public void success_commitForce_refreshNone() {
		doTestSuccess( DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
	}

	@Test
	public void success_commitForce_refreshForce() {
		doTestSuccess( DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE );
	}

	private void doTestSuccess(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet workSet = new LuceneIndexingPlanWriteWorkSet(
				createWorkMocks( 3 ), workSetFuture,
				commitStrategy, refreshStrategy
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		resetAll();
		processorMock.beforeWorkSet( commitStrategy, refreshStrategy );
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 2 ) ) ).andReturn( null );
		processorMock.afterSuccessfulWorkSet();
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful();
	}

	@Test
	public void markAsFailed() {
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet workSet = new LuceneIndexingPlanWriteWorkSet(
				createWorkMocks( 3 ), workSetFuture,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		Throwable throwable = new Throwable( "Some message" );
		resetAll();
		// Do not expect any call on the mocks
		replayAll();
		workSet.markAsFailed( throwable );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isFailed( throwable );
	}

	@Test
	public void failure_work() {
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet workSet = new LuceneIndexingPlanWriteWorkSet(
				createWorkMocks( 3 ), workSetFuture,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException workException = new RuntimeException( "Some message" );
		resetAll();
		processorMock.beforeWorkSet( DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andThrow( workException );
		expectWorkGetInfo( 0, 1, 2 );
		expect( processorMock.getFailureHandler() ).andReturn( failureHandlerMock );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		IndexFailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( workException );
		assertThat( failureContext.getFailingOperation() )
				.isEqualTo( workInfo( 1 ) );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// All works from the current workset, even successful ones
						workInfo( 0 ), workInfo( 1 ), workInfo( 2 )
				);
		FutureAssert.assertThat( workSetFuture ).isFailed( workException );
	}

	@Test
	public void failure_commit() {
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet workSet = new LuceneIndexingPlanWriteWorkSet(
				createWorkMocks( 3 ), workSetFuture,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		Capture<IndexFailureContext> failureContextCapture = Capture.newInstance();
		RuntimeException commitException = new RuntimeException( "Some message" );
		resetAll();
		processorMock.beforeWorkSet( DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 2 ) ) ).andReturn( null );
		processorMock.afterSuccessfulWorkSet();
		expectLastCall().andThrow( commitException );
		expectWorkGetInfo( 0, 1, 2 );
		expect( processorMock.getFailureHandler() ).andReturn( failureHandlerMock );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		IndexFailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( commitException );
		assertThat( failureContext.getFailingOperation() ).asString()
				.isEqualTo( "Commit after a set of index works" );
		Assertions.<Object>assertThat( failureContext.getUncommittedOperations() )
				.containsExactly(
						// All works from the current workset, even successful ones
						workInfo( 0 ), workInfo( 1 ), workInfo( 2 )
				);
		FutureAssert.assertThat( workSetFuture ).isFailed( commitException );
	}

	private void expectWorkGetInfo(int ... ids) {
		for ( int id : ids ) {
			LuceneWriteWork<?> workMock = workMocks.get( id );
			EasyMock.expect( workMock.getInfo() ).andStubReturn( workInfo( id ) );
		}
	}

	private List<LuceneWriteWork<?>> createWorkMocks(int count) {
		List<LuceneWriteWork<?>> result = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			result.add( createWorkMock() );
		}
		return result;
	}

	private <T> LuceneWriteWork<T> createWorkMock() {
		String workName = workInfo( workMocks.size() );
		LuceneWriteWork<T> workMock = createStrictMock( workName, LuceneWriteWork.class );
		workMocks.add( workMock );
		return workMock;
	}

	private String workInfo(int index) {
		return "work_" + index;
	}

}