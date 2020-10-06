/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.junit.Assume.assumeTrue;

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
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@RunWith(Parameterized.class)
@SuppressWarnings("unchecked") // Raw types are the only way to mock parameterized types
public class LuceneIndexIndexingPlanExecutionTest {

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

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	@Mock
	private LuceneSerialWorkOrchestrator orchestratorMock;

	@Mock(lenient = true)
	private EntityReferenceFactory<StubEntityReference> entityReferenceFactoryMock;

	private final List<SingleDocumentIndexingWork> workMocks = new ArrayList<>();

	public LuceneIndexIndexingPlanExecutionTest(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Before
	public void setup() {
		when( entityReferenceFactoryMock.createEntityReference( eq( TYPE_NAME ), any() ) )
				.thenAnswer( invocation -> entityReference( invocation.getArgument( 1 ) ) );
	}

	@Test
	public void success() {
		Long work1Result = 42L;
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Long>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work3FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				workMocks( 3 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute();
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ) );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work2FutureCaptor.getValue().complete( work2Result );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work1FutureCaptor.getValue().complete( work1Result );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work3FutureCaptor.getValue().complete( work3Result );
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			verify( orchestratorMock ).forceCommitInCurrentThread();
		}
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			verify( orchestratorMock ).forceRefreshInCurrentThread();
		}
		verifyNoOtherOrchestratorInteractionsAndReset();

		assertThatFuture( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).isEmpty();
				softly.assertThat( report.failingEntityReferences() ).isEmpty();
			} );
		} );
	}

	@Test
	public void failure_work() {
		RuntimeException work1Exception = new RuntimeException( "work1" );
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Long>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work3FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				workMocks( 3 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute();
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ) );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work2FutureCaptor.getValue().complete( work2Result );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work1FutureCaptor.getValue().completeExceptionally( work1Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work3FutureCaptor.getValue().complete( work3Result );
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			verify( orchestratorMock ).forceCommitInCurrentThread();
		}
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			verify( orchestratorMock ).forceRefreshInCurrentThread();
		}
		verifyNoOtherOrchestratorInteractionsAndReset();

		assertThatFuture( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).containsSame( work1Exception );
				softly.assertThat( report.failingEntityReferences() )
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
		ArgumentCaptor<CompletableFuture<Long>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work3FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work4FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				workMocks( 4 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute();
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ) );
		verify( orchestratorMock ).submit( work4FutureCaptor.capture(), eq( workMocks.get( 3 ) ) );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work2FutureCaptor.getValue().complete( work2Result );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work1FutureCaptor.getValue().completeExceptionally( work1Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work3FutureCaptor.getValue().completeExceptionally( work3Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work4FutureCaptor.getValue().complete( work4Result );
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			verify( orchestratorMock ).forceCommitInCurrentThread();
		}
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			verify( orchestratorMock ).forceRefreshInCurrentThread();
		}
		verifyNoOtherOrchestratorInteractionsAndReset();

		assertThatFuture( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).containsSame( work1Exception );
				assertThat( work1Exception ).hasSuppressedException( work3Exception );
				softly.assertThat( report.failingEntityReferences() )
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
		ArgumentCaptor<CompletableFuture<Long>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work3FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work4FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				workMocks( 4 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute();
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ) );
		verify( orchestratorMock ).submit( work4FutureCaptor.capture(), eq( workMocks.get( 3 ) ) );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work2FutureCaptor.getValue().complete( work2Result );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work1FutureCaptor.getValue().completeExceptionally( work1Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work3FutureCaptor.getValue().completeExceptionally( work3Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		RuntimeException entityReferenceFactoryException = new RuntimeException( "EntityReferenceFactory message" );
		when( entityReferenceFactoryMock.createEntityReference( TYPE_NAME, 0 ) )
				.thenThrow( entityReferenceFactoryException );
		work4FutureCaptor.getValue().complete( work4Result );
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			verify( orchestratorMock ).forceCommitInCurrentThread();
		}
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			verify( orchestratorMock ).forceRefreshInCurrentThread();
		}
		verifyNoOtherOrchestratorInteractionsAndReset();

		assertThatFuture( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).containsSame( work1Exception );
				softly.assertThat( work1Exception ).hasSuppressedException( entityReferenceFactoryException );
				softly.assertThat( report.failingEntityReferences() )
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
		assumeTrue(
				"This test only makes sense when commit is forced",
				DocumentCommitStrategy.FORCE.equals( commitStrategy )
		);

		Long work1Result = 42L;
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Long>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work3FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				workMocks( 3 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute();
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ) );
		verifyNoOtherOrchestratorInteractionsAndReset();

		work2FutureCaptor.getValue().complete( work2Result );
		work1FutureCaptor.getValue().complete( work1Result );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		// Fail upon commit
		RuntimeException commitException = new RuntimeException( "Some message" );
		doThrow( commitException ).when( orchestratorMock ).forceCommitInCurrentThread();
		work3FutureCaptor.getValue().complete( work3Result );
		// ... no refresh expected, since the commit failed ...
		verifyNoOtherOrchestratorInteractionsAndReset();

		assertThatFuture( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).containsSame( commitException );
				softly.assertThat( report.failingEntityReferences() )
						.containsExactly(
								// All entities, even if their work succeeded
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	@Test
	public void failure_refresh() {
		assumeTrue(
				"This test only makes sense when refresh is forced",
				DocumentRefreshStrategy.FORCE.equals( refreshStrategy )
		);

		Long work1Result = 42L;
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Long>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work3FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, DocumentRefreshStrategy.FORCE,
				workMocks( 3 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute();
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ) );
		verifyNoOtherOrchestratorInteractionsAndReset();

		work2FutureCaptor.getValue().complete( work2Result );
		work1FutureCaptor.getValue().complete( work1Result );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		// Fail upon refresh
		RuntimeException refreshException = new RuntimeException( "Some message" );
		doThrow( refreshException ).when( orchestratorMock ).forceRefreshInCurrentThread();
		work3FutureCaptor.getValue().complete( work3Result );
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			verify( orchestratorMock ).forceCommitInCurrentThread();
		}
		verifyNoOtherOrchestratorInteractionsAndReset();

		assertThatFuture( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).containsSame( refreshException );
				softly.assertThat( report.failingEntityReferences() )
						.containsExactly(
								// All entities, even if their work succeeded
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	@Test
	public void failure_workAndCommit() {
		assumeTrue(
				"This test only makes sense when commit is forced",
				DocumentCommitStrategy.FORCE.equals( commitStrategy )
		);

		RuntimeException work1Exception = new RuntimeException( "work1" );
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Long>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work3FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, refreshStrategy,
				workMocks( 3 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute();
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ) );
		verifyNoOtherOrchestratorInteractionsAndReset();

		work2FutureCaptor.getValue().complete( work2Result );
		work1FutureCaptor.getValue().completeExceptionally( work1Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		// Fail upon commit
		RuntimeException commitException = new RuntimeException( "Some message" );
		doThrow( commitException ).when( orchestratorMock ).forceCommitInCurrentThread();
		work3FutureCaptor.getValue().complete( work3Result );
		// ... no refresh expected, since the commit failed ...
		verifyNoOtherOrchestratorInteractionsAndReset();

		assertThatFuture( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).containsSame( work1Exception );
				softly.assertThat( work1Exception ).hasSuppressedException( commitException );
				softly.assertThat( report.failingEntityReferences() )
						.containsExactly(
								// All entities, even if their work succeeded
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	@Test
	public void failure_workAndRefresh() {
		assumeTrue(
				"This test only makes sense when refresh is forced",
				DocumentRefreshStrategy.FORCE.equals( refreshStrategy )
		);

		RuntimeException work1Exception = new RuntimeException( "work1" );
		Long work2Result = 41L;
		Long work3Result = 43L;

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Long>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Long>> work3FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		LuceneIndexIndexingPlanExecution<StubEntityReference> execution = new LuceneIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
				commitStrategy, DocumentRefreshStrategy.FORCE,
				workMocks( 3 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute();
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ) );
		verifyNoOtherOrchestratorInteractionsAndReset();

		work2FutureCaptor.getValue().complete( work2Result );
		work1FutureCaptor.getValue().completeExceptionally( work1Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		// Fail upon refresh
		RuntimeException refreshException = new RuntimeException( "Some message" );
		doThrow( refreshException ).when( orchestratorMock ).forceRefreshInCurrentThread();
		work3FutureCaptor.getValue().complete( work3Result );
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) ) {
			verify( orchestratorMock ).forceCommitInCurrentThread();
		}
		verifyNoOtherOrchestratorInteractionsAndReset();

		assertThatFuture( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).containsSame( work1Exception );
				softly.assertThat( work1Exception ).hasSuppressedException( refreshException );
				softly.assertThat( report.failingEntityReferences() )
						.containsExactly(
								// All entities, even if their work succeeded
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	private void verifyNoOtherOrchestratorInteractionsAndReset() {
		verifyNoMoreInteractions( orchestratorMock );
		reset( orchestratorMock );
	}

	private List<SingleDocumentIndexingWork> workMocks(int count) {
		List<SingleDocumentIndexingWork> result = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			result.add( workMock() );
		}
		return result;
	}

	private <T> ArgumentCaptor<CompletableFuture<T>> futureCaptor() {
		return ArgumentCaptor.forClass( CompletableFuture.class );
	}

	private SingleDocumentIndexingWork workMock() {
		int id = workMocks.size();
		String workName = workInfo( id );
		SingleDocumentIndexingWork workMock = mock( SingleDocumentIndexingWork.class,
				withSettings().name( workName ).lenient() );
		when( workMock.getInfo() ).thenReturn( workName );
		when( workMock.getEntityTypeName() ).thenReturn( TYPE_NAME );
		when( workMock.getEntityIdentifier() ).thenReturn( id );
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