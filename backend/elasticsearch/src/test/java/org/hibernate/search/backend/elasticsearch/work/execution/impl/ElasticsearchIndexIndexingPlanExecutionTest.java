/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSerialWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

import org.assertj.core.api.SoftAssertions;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class ElasticsearchIndexIndexingPlanExecutionTest extends EasyMockSupport {

	private static final String TYPE_NAME = "SomeTypeName";

	private final ElasticsearchSerialWorkOrchestrator orchestratorMock = createStrictMock( ElasticsearchSerialWorkOrchestrator.class );

	private final EntityReferenceFactory<StubEntityReference> entityReferenceFactoryMock =
			createStrictMock( EntityReferenceFactory.class );

	private final List<SingleDocumentIndexingWork> workMocks = new ArrayList<>();

	@Test
	public void success() {
		// Work futures: we will complete them
		Capture<CompletableFuture<Void>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work3FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		ElasticsearchIndexIndexingPlanExecution<StubEntityReference> execution = new ElasticsearchIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
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
		work2FutureCapture.getValue().complete( null );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work1FutureCapture.getValue().complete( null );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work3FutureCapture.getValue().complete( null );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).isEmpty();
				softly.assertThat( report.failingEntityReferences() ).isEmpty();
			} );
		} );
	}

	@Test
	public void failure_work() {
		RuntimeException work1Exception = new RuntimeException( "work1" );

		// Work futures: we will complete them
		Capture<CompletableFuture<Void>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work3FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		ElasticsearchIndexIndexingPlanExecution<StubEntityReference> execution = new ElasticsearchIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
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
		work2FutureCapture.getValue().complete( null );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		replayAll();
		work1FutureCapture.getValue().completeExceptionally( work1Exception );
		verifyAll();
		FutureAssert.assertThat( planExecutionFuture ).isPending();

		resetAll();
		expectWorkGetInfo( 0 );
		replayAll();
		work3FutureCapture.getValue().complete( null );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
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
		Capture<CompletableFuture<Void>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work3FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work4FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		ElasticsearchIndexIndexingPlanExecution<StubEntityReference> execution = new ElasticsearchIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
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
		work2FutureCapture.getValue().complete( null );
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
		expectWorkGetInfo( 0, 2 );
		replayAll();
		work4FutureCapture.getValue().complete( null );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
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
		Capture<CompletableFuture<Void>> work1FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work2FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work3FutureCapture = Capture.newInstance();
		Capture<CompletableFuture<Void>> work4FutureCapture = Capture.newInstance();

		// Plan future: we will test it
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> planExecutionFuture;

		resetAll();
		ElasticsearchIndexIndexingPlanExecution<StubEntityReference> execution = new ElasticsearchIndexIndexingPlanExecution<>(
				orchestratorMock,
				entityReferenceFactoryMock,
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
		work2FutureCapture.getValue().complete( null );
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
		expectFailingWorkGetInfo( 0, entityReferenceFactoryException );
		expectWorkGetInfo( 2 );
		replayAll();
		work4FutureCapture.getValue().complete( null );
		verifyAll();

		FutureAssert.assertThat( planExecutionFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
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

	private void expectWorkGetInfo(int ... ids) {
		for ( int id : ids ) {
			SingleDocumentIndexingWork workMock = workMocks.get( id );
			EasyMock.expect( workMock.getEntityTypeName() ).andStubReturn( TYPE_NAME );
			EasyMock.expect( workMock.getEntityIdentifier() ).andStubReturn( id );
			EasyMock.expect( entityReferenceFactoryMock.createEntityReference( TYPE_NAME, id ) )
					.andReturn( entityReference( id ) );
		}
	}

	private void expectFailingWorkGetInfo(int id, Throwable thrown) {
		SingleDocumentIndexingWork workMock = workMocks.get( id );
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