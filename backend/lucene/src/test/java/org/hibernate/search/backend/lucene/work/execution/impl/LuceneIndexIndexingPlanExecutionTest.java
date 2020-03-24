/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.SoftAssertions;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

@RunWith(Parameterized.class)
public class LuceneIndexIndexingPlanExecutionTest extends EasyMockSupport {

	private static final String TYPE_NAME = "SomeTypeName";

	@Parameterized.Parameters(name = "commit = {0}, refresh = {1}")
	public static Object[][] params() {
		List<Object[]> params = new ArrayList<>();
		for ( DocumentCommitStrategy commitStrategy : DocumentCommitStrategy.values() ) {
			for ( DocumentRefreshStrategy refreshStrategy : DocumentRefreshStrategy.values() ) {
				params.add( new Object[] { commitStrategy, refreshStrategy } );
			}
		}
		return params.toArray( new Object[0][] );
	}

	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final LuceneSerialWorkOrchestrator orchestratorMock = createStrictMock( LuceneSerialWorkOrchestrator.class );

	private final EntityReferenceFactory<StubEntityReference> entityReferenceFactoryMock =
			createStrictMock( EntityReferenceFactory.class );

	private final List<SingleDocumentIndexingWork> workMocks = new ArrayList<>();

	public LuceneIndexIndexingPlanExecutionTest(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Test
	public void success() {
		Long work1Result = 42L;
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		Capture<CompletableFuture<Long>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work3FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				createWorkMocks( 3 )
		);
		replayAll();
		verifyAll();

		resetAll();
		orchestratorMock.submit( capture( work1FutureCapture ), eq( workMocks.get( 0 ) ) );
		orchestratorMock.submit( capture( work2FutureCapture ), eq( workMocks.get( 1 ) ) );
		orchestratorMock.submit( capture( work3FutureCapture ), eq( workMocks.get( 2 ) ) );
		replayAll();
		planExecutionFuture = execution.execute();
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work2FutureCapture.getValue().complete( work2Result );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work1FutureCapture.getValue().complete( work1Result );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			orchestratorMock.forceCommitInCurrentThread();
		}
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			orchestratorMock.forceRefreshInCurrentThread();
		}
		replayAll();
		work3FutureCapture.getValue().complete( work3Result );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).isEmpty();
				softly.assertThat( report.getFailingEntityReferences() ).isEmpty();
			} );
		} );
	}

	@Test
	public void failure_work() {
		RuntimeException work1Exception = new RuntimeException( "work1" );
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		Capture<CompletableFuture<Long>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work3FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				createWorkMocks( 3 )
		);
		replayAll();
		verifyAll();

		resetAll();
		orchestratorMock.submit( capture( work1FutureCapture ), eq( workMocks.get( 0 ) ) );
		orchestratorMock.submit( capture( work2FutureCapture ), eq( workMocks.get( 1 ) ) );
		orchestratorMock.submit( capture( work3FutureCapture ), eq( workMocks.get( 2 ) ) );
		replayAll();
		planExecutionFuture = execution.execute();
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work2FutureCapture.getValue().complete( work2Result );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work1FutureCapture.getValue().completeExceptionally( work1Exception );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			orchestratorMock.forceCommitInCurrentThread();
		}
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			orchestratorMock.forceRefreshInCurrentThread();
		}
		expectWorkGetInfo( 0 );
		replayAll();
		work3FutureCapture.getValue().complete( work3Result );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( work1Exception );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactlyInAnyOrder( entityReference( 0 ) );
			} );
		} );
	}

	@Test
	public void failure_multipleWorks() {
		RuntimeException work1Exception = new RuntimeException( "work1" );
		Long work2Result = 41L;
		RuntimeException work3Exception = new RuntimeException( "work3" );
		Long work4Result = 44L;

		// Work futures: we will complete them
		Capture<CompletableFuture<Long>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work3FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work4FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				createWorkMocks( 4 )
		);
		replayAll();
		verifyAll();

		resetAll();
		orchestratorMock.submit( capture( work1FutureCapture ), eq( workMocks.get( 0 ) ) );
		orchestratorMock.submit( capture( work2FutureCapture ), eq( workMocks.get( 1 ) ) );
		orchestratorMock.submit( capture( work3FutureCapture ), eq( workMocks.get( 2 ) ) );
		orchestratorMock.submit( capture( work4FutureCapture ), eq( workMocks.get( 3 ) ) );
		replayAll();
		planExecutionFuture = execution.execute();
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work2FutureCapture.getValue().complete( work2Result );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work1FutureCapture.getValue().completeExceptionally( work1Exception );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work3FutureCapture.getValue().completeExceptionally( work3Exception );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			orchestratorMock.forceCommitInCurrentThread();
		}
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			orchestratorMock.forceRefreshInCurrentThread();
		}
		expectWorkGetInfo( 0, 2 );
		replayAll();
		work4FutureCapture.getValue().complete( work4Result );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( work1Exception );
				assertThat( work1Exception ).hasSuppressedException( work3Exception );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactlyInAnyOrder( entityReference( 0 ), entityReference( 2 ) );
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3851")
	public void failure_multipleWorksAndCreateEntityReference() {
		RuntimeException work1Exception = new RuntimeException( "work1" );
		Long work2Result = 41L;
		RuntimeException work3Exception = new RuntimeException( "work3" );
		Long work4Result = 44L;

		// Work futures: we will complete them
		Capture<CompletableFuture<Long>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work3FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work4FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				createWorkMocks( 4 )
		);
		replayAll();
		verifyAll();

		resetAll();
		orchestratorMock.submit( capture( work1FutureCapture ), eq( workMocks.get( 0 ) ) );
		orchestratorMock.submit( capture( work2FutureCapture ), eq( workMocks.get( 1 ) ) );
		orchestratorMock.submit( capture( work3FutureCapture ), eq( workMocks.get( 2 ) ) );
		orchestratorMock.submit( capture( work4FutureCapture ), eq( workMocks.get( 3 ) ) );
		replayAll();
		planExecutionFuture = execution.execute();
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work2FutureCapture.getValue().complete( work2Result );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work1FutureCapture.getValue().completeExceptionally( work1Exception );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work3FutureCapture.getValue().completeExceptionally( work3Exception );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		RuntimeException entityReferenceFactoryException = new RuntimeException( "EntityReferenceFactory message" );
		resetAll();
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			orchestratorMock.forceCommitInCurrentThread();
		}
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			orchestratorMock.forceRefreshInCurrentThread();
		}
		expectFailingWorkGetInfo( 0, entityReferenceFactoryException );
		expectWorkGetInfo( 2 );
		replayAll();
		work4FutureCapture.getValue().complete( work4Result );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( work1Exception );
				softly.assertThat( work1Exception ).hasSuppressedException( entityReferenceFactoryException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// Reference to entity 0 is missing because we could not create it,
								// but at least the other references are reported.
								entityReference( 2 )
						);
			} );
		} );
	}

	@Test
	public void failure_commit() {
		Assume.assumeTrue(
				"This test only makes sense when commit is forced",
				DocumentCommitStrategy.FORCE.equals( commitStrategy )
		);

		Long work1Result = 42L;
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		Capture<CompletableFuture<Long>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work3FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				createWorkMocks( 3 )
		);
		replayAll();
		verifyAll();

		resetAll();
		orchestratorMock.submit( capture( work1FutureCapture ), eq( workMocks.get( 0 ) ) );
		orchestratorMock.submit( capture( work2FutureCapture ), eq( workMocks.get( 1 ) ) );
		orchestratorMock.submit( capture( work3FutureCapture ), eq( workMocks.get( 2 ) ) );
		replayAll();
		planExecutionFuture = execution.execute();
		verifyAll();

		resetAll();
		replayAll();
		work2FutureCapture.getValue().complete( work2Result );
		work1FutureCapture.getValue().complete( work1Result );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		// Fail upon commit
		RuntimeException commitException = new RuntimeException( "Some message" );
		resetAll();
		orchestratorMock.forceCommitInCurrentThread();
		expectLastCall().andThrow( commitException );
		// ... no refresh expected, since the commit failed ...
		expectWorkGetInfo( 0, 1, 2 );
		replayAll();
		work3FutureCapture.getValue().complete( work3Result );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( commitException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// All entities, even if their work succeeded
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	@Test
	public void failure_refresh() {
		Assume.assumeTrue(
				"This test only makes sense when refresh is forced",
				DocumentRefreshStrategy.FORCE.equals( refreshStrategy )
		);

		Long work1Result = 42L;
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		Capture<CompletableFuture<Long>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work3FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, DocumentRefreshStrategy.FORCE,
				createWorkMocks( 3 )
		);
		replayAll();
		verifyAll();

		resetAll();
		orchestratorMock.submit( capture( work1FutureCapture ), eq( workMocks.get( 0 ) ) );
		orchestratorMock.submit( capture( work2FutureCapture ), eq( workMocks.get( 1 ) ) );
		orchestratorMock.submit( capture( work3FutureCapture ), eq( workMocks.get( 2 ) ) );
		replayAll();
		planExecutionFuture = execution.execute();
		verifyAll();

		resetAll();
		replayAll();
		work2FutureCapture.getValue().complete( work2Result );
		work1FutureCapture.getValue().complete( work1Result );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		// Fail upon refresh
		RuntimeException refreshException = new RuntimeException( "Some message" );
		resetAll();
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			orchestratorMock.forceCommitInCurrentThread();
		}
		orchestratorMock.forceRefreshInCurrentThread();
		expectLastCall().andThrow( refreshException );
		expectWorkGetInfo( 0, 1, 2 );
		replayAll();
		work3FutureCapture.getValue().complete( work3Result );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( refreshException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// All entities, even if their work succeeded
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}


	@Test
	public void failure_workAndCommit() {
		Assume.assumeTrue(
				"This test only makes sense when commit is forced",
				DocumentCommitStrategy.FORCE.equals( commitStrategy )
		);

		RuntimeException work1Exception = new RuntimeException( "work1" );
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		Capture<CompletableFuture<Long>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work3FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				createWorkMocks( 3 )
		);
		replayAll();
		verifyAll();

		resetAll();
		orchestratorMock.submit( capture( work1FutureCapture ), eq( workMocks.get( 0 ) ) );
		orchestratorMock.submit( capture( work2FutureCapture ), eq( workMocks.get( 1 ) ) );
		orchestratorMock.submit( capture( work3FutureCapture ), eq( workMocks.get( 2 ) ) );
		replayAll();
		planExecutionFuture = execution.execute();
		verifyAll();

		resetAll();
		replayAll();
		work2FutureCapture.getValue().complete( work2Result );
		work1FutureCapture.getValue().completeExceptionally( work1Exception );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		// Fail upon commit
		RuntimeException commitException = new RuntimeException( "Some message" );
		resetAll();
		orchestratorMock.forceCommitInCurrentThread();
		expectLastCall().andThrow( commitException );
		// ... no refresh expected, since the commit failed ...
		expectWorkGetInfo( 0, 1, 2 );
		replayAll();
		work3FutureCapture.getValue().complete( work3Result );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( work1Exception );
				softly.assertThat( work1Exception ).hasSuppressedException( commitException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// All entities, even if their work succeeded
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	@Test
	public void failure_workAndRefresh() {
		Assume.assumeTrue(
				"This test only makes sense when refresh is forced",
				DocumentRefreshStrategy.FORCE.equals( refreshStrategy )
		);

		RuntimeException work1Exception = new RuntimeException( "work1" );
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		Capture<CompletableFuture<Long>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Long>> work3FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, DocumentRefreshStrategy.FORCE,
				createWorkMocks( 3 )
		);
		replayAll();
		verifyAll();

		resetAll();
		orchestratorMock.submit( capture( work1FutureCapture ), eq( workMocks.get( 0 ) ) );
		orchestratorMock.submit( capture( work2FutureCapture ), eq( workMocks.get( 1 ) ) );
		orchestratorMock.submit( capture( work3FutureCapture ), eq( workMocks.get( 2 ) ) );
		replayAll();
		planExecutionFuture = execution.execute();
		verifyAll();

		resetAll();
		replayAll();
		work2FutureCapture.getValue().complete( work2Result );
		work1FutureCapture.getValue().completeExceptionally( work1Exception );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		// Fail upon refresh
		RuntimeException refreshException = new RuntimeException( "Some message" );
		resetAll();
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			orchestratorMock.forceCommitInCurrentThread();
		}
		orchestratorMock.forceRefreshInCurrentThread();
		expectLastCall().andThrow( refreshException );
		expectWorkGetInfo( 0, 1, 2 );
		replayAll();
		work3FutureCapture.getValue().complete( work3Result );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( work1Exception );
				softly.assertThat( work1Exception ).hasSuppressedException( refreshException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// All entities, even if their work succeeded
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	private void expectWorkGetInfo(int ... ids) {
		for ( int id : ids ) {
			SingleDocumentIndexingWork workMock = workMocks.get( id );
			EasyMock.expect( workMock.getInfo() ).andStubReturn( workInfo( id ) );
			EasyMock.expect( workMock.getEntityTypeName() ).andStubReturn( TYPE_NAME );
			EasyMock.expect( workMock.getEntityIdentifier() ).andStubReturn( id );
			EasyMock.expect( entityReferenceFactoryMock.createEntityReference( TYPE_NAME, id ) )
					.andReturn( entityReference( id ) );
		}
	}

	private void expectFailingWorkGetInfo(int id, Throwable thrown) {
		SingleDocumentIndexingWork workMock = workMocks.get( id );
		EasyMock.expect( workMock.getInfo() ).andStubReturn( workInfo( id ) );
		EasyMock.expect( workMock.getEntityTypeName() ).andStubReturn( TYPE_NAME );
		EasyMock.expect( workMock.getEntityIdentifier() ).andStubReturn( id );
		EasyMock.expect( entityReferenceFactoryMock.createEntityReference( TYPE_NAME, id ) )
				.andThrow( thrown );
	}

	private List<SingleDocumentIndexingWork> createWorkMocks(int count) {
		List<SingleDocumentIndexingWork> result = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			result.add( createWorkMock() );
		}
		return result;
	}

	private <T> SingleDocumentIndexingWork createWorkMock() {
		String workName = workInfo( workMocks.size() );
		SingleDocumentIndexingWork workMock = createStrictMock( workName, SingleDocumentIndexingWork.class );
		workMocks.add( workMock );
		return workMock;
	}

	private StubEntityReference entityReference(int id) {
		return new StubEntityReference( TYPE_NAME, id );
	}

	private String workInfo(int index) {
		return "work_" + index;
	}

	private static class StubEntityReference {
		private final String typeName;
		private final Object identifier;

		private StubEntityReference(String typeName, Object identifier) {
			this.typeName = typeName;
			this.identifier = identifier;
		}

		@Override
		public String toString() {
			return "StubEntityReference{" +
					"typeName='" + typeName + '\'' +
					", identifier=" + identifier +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			StubEntityReference that = (StubEntityReference) o;
			return Objects.equals( typeName, that.typeName ) &&
					Objects.equals( identifier, that.identifier );
		}

		@Override
		public int hashCode() {
			return Objects.hash( typeName, identifier );
		}
	}
}