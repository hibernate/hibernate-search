/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSerialWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings("unchecked") // Raw types are the only way to mock parameterized types
public class ElasticsearchIndexIndexingPlanExecutionTest {

	private static final String TYPE_NAME = "SomeTypeName";

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private ElasticsearchSerialWorkOrchestrator orchestratorMock;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private EntityReferenceFactory entityReferenceFactoryMock;

	private final List<SingleDocumentIndexingWork> workMocks = new ArrayList<>();

	@Before
	public void setup() {
		when( entityReferenceFactoryMock.createEntityReference( eq( TYPE_NAME ), any() ) )
				.thenAnswer( invocation -> entityReference( invocation.getArgument( 1 ) ) );
	}

	@Test
	public void success() {
		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Void>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work3FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<MultiEntityOperationExecutionReport> planExecutionFuture;

		ElasticsearchIndexIndexingPlanExecution execution = new ElasticsearchIndexIndexingPlanExecution(
				orchestratorMock,
				entityReferenceFactoryMock,
				workMocks( 3 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute( OperationSubmitter.blocking() );
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ),
				eq( OperationSubmitter.blocking() ) );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work2FutureCaptor.getValue().complete( null );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work1FutureCaptor.getValue().complete( null );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work3FutureCaptor.getValue().complete( null );
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

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Void>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work3FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<MultiEntityOperationExecutionReport> planExecutionFuture;

		ElasticsearchIndexIndexingPlanExecution execution = new ElasticsearchIndexIndexingPlanExecution(
				orchestratorMock,
				entityReferenceFactoryMock,
				workMocks( 3 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute( OperationSubmitter.blocking() );
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ),
				eq( OperationSubmitter.blocking() ) );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work2FutureCaptor.getValue().complete( null );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work1FutureCaptor.getValue().completeExceptionally( work1Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work3FutureCaptor.getValue().complete( null );
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
		RuntimeException work3Exception = new RuntimeException( "work3" );

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Void>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work3FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work4FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<MultiEntityOperationExecutionReport> planExecutionFuture;

		ElasticsearchIndexIndexingPlanExecution execution = new ElasticsearchIndexIndexingPlanExecution(
				orchestratorMock,
				entityReferenceFactoryMock,
				workMocks( 4 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute( OperationSubmitter.blocking() );
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work4FutureCaptor.capture(), eq( workMocks.get( 3 ) ),
				eq( OperationSubmitter.blocking() ) );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work2FutureCaptor.getValue().complete( null );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work1FutureCaptor.getValue().completeExceptionally( work1Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work3FutureCaptor.getValue().completeExceptionally( work3Exception );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work4FutureCaptor.getValue().complete( null );
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
		RuntimeException work3Exception = new RuntimeException( "work3" );

		// Work futures: we will complete them
		ArgumentCaptor<CompletableFuture<Void>> work1FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work2FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work3FutureCaptor = futureCaptor();
		ArgumentCaptor<CompletableFuture<Void>> work4FutureCaptor = futureCaptor();

		// Plan future: we will test it
		CompletableFuture<MultiEntityOperationExecutionReport> planExecutionFuture;

		ElasticsearchIndexIndexingPlanExecution execution = new ElasticsearchIndexIndexingPlanExecution(
				orchestratorMock,
				entityReferenceFactoryMock,
				workMocks( 4 )
		);
		verifyNoOtherOrchestratorInteractionsAndReset();

		planExecutionFuture = execution.execute( OperationSubmitter.blocking() );
		verify( orchestratorMock ).submit( work1FutureCaptor.capture(), eq( workMocks.get( 0 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work2FutureCaptor.capture(), eq( workMocks.get( 1 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work3FutureCaptor.capture(), eq( workMocks.get( 2 ) ),
				eq( OperationSubmitter.blocking() ) );
		verify( orchestratorMock ).submit( work4FutureCaptor.capture(), eq( workMocks.get( 3 ) ),
				eq( OperationSubmitter.blocking() ) );
		verifyNoOtherOrchestratorInteractionsAndReset();
		assertThatFuture( planExecutionFuture ).isPending();

		work2FutureCaptor.getValue().complete( null );
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
		work4FutureCaptor.getValue().complete( null );
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
				withSettings().name( workName ).strictness( Strictness.LENIENT ) );
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

	private static class StubEntityReference implements EntityReference {
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
			return Objects.equals( typeName, that.typeName )
					&& Objects.equals( identifier, that.identifier );
		}

		@Override
		public int hashCode() {
			return Objects.hash( typeName, identifier );
		}

		@Override
		public Class<?> type() {
			return Object.class;
		}

		@Override
		public String name() {
			return typeName;
		}

		@Override
		public Object id() {
			return identifier;
		}
	}
}
