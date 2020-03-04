/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentElasticsearchWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Test;

import org.assertj.core.api.SoftAssertions;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

/**
 * Test worksets produced by indexing plans.
 */
public class ElasticsearchIndexingPlanWorkSetTest extends EasyMockSupport {

	private static final String TYPE_NAME = "SomeTypeName";

	private ElasticsearchWorkProcessor processorMock = createStrictMock( ElasticsearchWorkProcessor.class );

	private List<SingleDocumentElasticsearchWork<Void>> workMocks = new ArrayList<>();

	private EntityReferenceFactory<StubEntityReference> entityReferenceFactoryMock =
			createStrictMock( EntityReferenceFactory.class );

	@Test
	public void success() {
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet<StubEntityReference> workSet = new ElasticsearchIndexingPlanWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		resetAll();
		processorMock.beforeWorkSet();
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( work0Future );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andReturn( work1Future );
		expect( processorMock.submit( workMocks.get( 2 ) ) ).andReturn( work2Future );
		expect( processorMock.afterWorkSet() ).andReturn( workSequenceFuture );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isPending();

		resetAll();
		// Don't expect any call on mocks
		replayAll();
		work0Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work1Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work2Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		workSequenceFuture.complete( null );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).isEmpty();
				softly.assertThat( report.getFailingEntityReferences() ).isEmpty();
			} );
		} );
	}

	@Test
	public void markAsFailed() {
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet<StubEntityReference> workSet = new ElasticsearchIndexingPlanWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture
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
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet<StubEntityReference> workSet = new ElasticsearchIndexingPlanWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		resetAll();
		processorMock.beforeWorkSet();
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( work0Future );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andReturn( work1Future );
		expect( processorMock.submit( workMocks.get( 2 ) ) ).andReturn( work2Future );
		expect( processorMock.afterWorkSet() ).andReturn( workSequenceFuture );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isPending();

		RuntimeException workException = new RuntimeException( "Some message" );
		resetAll();
		expectWorkGetInfo( 1 );
		replayAll();
		work0Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work1Future.completeExceptionally( workException );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work2Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		// If a work fails, the sequence future will be completed with the same exception
		workSequenceFuture.completeExceptionally( workException );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( workException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// Only documents whose indexing failed
								entityReference( 1 )
						);
			} );
		} );
	}

	@Test
	public void failure_multipleWorks() {
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> work4Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet<StubEntityReference> workSet = new ElasticsearchIndexingPlanWorkSet<>(
				createWorkMocks( 5 ), entityReferenceFactoryMock, workSetFuture
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		resetAll();
		processorMock.beforeWorkSet();
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( work0Future );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andReturn( work1Future );
		expect( processorMock.submit( workMocks.get( 2 ) ) ).andReturn( work2Future );
		expect( processorMock.submit( workMocks.get( 3 ) ) ).andReturn( work3Future );
		expect( processorMock.submit( workMocks.get( 4 ) ) ).andReturn( work4Future );
		expect( processorMock.afterWorkSet() ).andReturn( workSequenceFuture );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isPending();

		RuntimeException work1Exception = new RuntimeException( "Some message" );
		RuntimeException work3Exception = new RuntimeException( "Some message" );
		resetAll();
		expectWorkGetInfo( 1, 3 );
		replayAll();
		work0Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work1Future.completeExceptionally( work1Exception );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work2Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work3Future.completeExceptionally( work3Exception );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work4Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		// If a work fails, the sequence future will be completed with the same exception
		workSequenceFuture.completeExceptionally( work1Exception );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( work1Exception );
				softly.assertThat( work1Exception ).hasSuppressedException( work3Exception );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// Only documents whose indexing failed
								entityReference( 1 ), entityReference( 3 )
						);
			} );
		} );
	}

	@Test
	public void failure_sequence() {
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet<StubEntityReference> workSet = new ElasticsearchIndexingPlanWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		resetAll();
		processorMock.beforeWorkSet();
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( work0Future );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andReturn( work1Future );
		expect( processorMock.submit( workMocks.get( 2 ) ) ).andReturn( work2Future );
		expect( processorMock.afterWorkSet() ).andReturn( workSequenceFuture );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isPending();

		RuntimeException sequenceException = new RuntimeException( "Some other message" );
		resetAll();
		replayAll();
		work0Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work1Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work2Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		// Let's say the sequence fails, for example because forcing refresh failed.
		workSequenceFuture.completeExceptionally( sequenceException );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( sequenceException );
				softly.assertThat( report.getFailingEntityReferences() )
						// No indexing failed. Happens for example when forcing refresh fails.
						.isEmpty();
			} );
		} );
	}

	@Test
	public void failure_workAndSequence() {
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet<StubEntityReference> workSet = new ElasticsearchIndexingPlanWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		resetAll();
		processorMock.beforeWorkSet();
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( work0Future );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andReturn( work1Future );
		expect( processorMock.submit( workMocks.get( 2 ) ) ).andReturn( work2Future );
		expect( processorMock.afterWorkSet() ).andReturn( workSequenceFuture );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isPending();

		RuntimeException workException = new RuntimeException( "Some message" );
		RuntimeException sequenceException = new RuntimeException( "Some other message" );
		resetAll();
		expectWorkGetInfo( 1 );
		replayAll();
		work0Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work1Future.completeExceptionally( workException );
		FutureAssert.assertThat( workSetFuture ).isPending();
		work2Future.complete( null );
		FutureAssert.assertThat( workSetFuture ).isPending();
		// Let's say the sequence fails too, with a different exception
		workSequenceFuture.completeExceptionally( sequenceException );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( workException );
				softly.assertThat( workException ).hasSuppressedException( sequenceException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// Only documents whose indexing failed
								entityReference( 1 )
						);
			} );
		} );
	}

	private void expectWorkGetInfo(int ... ids) {
		for ( int id : ids ) {
			SingleDocumentElasticsearchWork<?> workMock = workMocks.get( id );
			EasyMock.expect( workMock.getInfo() ).andStubReturn( workInfo( id ) );
			EasyMock.expect( workMock.getEntityTypeName() ).andStubReturn( TYPE_NAME );
			EasyMock.expect( workMock.getEntityIdentifier() ).andStubReturn( id );
			EasyMock.expect( entityReferenceFactoryMock.createEntityReference( TYPE_NAME, id ) )
					.andReturn( entityReference( id ) );
		}
	}

	private List<SingleDocumentElasticsearchWork<?>> createWorkMocks(int count) {
		List<SingleDocumentElasticsearchWork<?>> result = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			result.add( createWorkMock() );
		}
		return result;
	}

	private SingleDocumentElasticsearchWork<Void> createWorkMock() {
		String workName = workInfo( workMocks.size() );
		SingleDocumentElasticsearchWork<Void> workMock = createStrictMock( workName, SingleDocumentElasticsearchWork.class );
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