/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkProcessor;
import org.hibernate.search.backend.lucene.work.impl.LuceneSingleDocumentWriteWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

import org.assertj.core.api.SoftAssertions;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

/**
 * Test worksets produced by indexing plans.
 */
public class LuceneIndexingPlanWriteWorkSetTest extends EasyMockSupport {

	private static final String TYPE_NAME = "SomeTypeName";

	private LuceneWriteWorkProcessor processorMock = createStrictMock( LuceneWriteWorkProcessor.class );

	private EntityReferenceFactory<StubEntityReference> entityReferenceFactoryMock =
			createStrictMock( EntityReferenceFactory.class );

	private List<LuceneSingleDocumentWriteWork> workMocks = new ArrayList<>();

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
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet<StubEntityReference> workSet = new LuceneIndexingPlanWriteWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture,
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

		LuceneIndexingPlanWriteWorkSet<StubEntityReference> workSet = new LuceneIndexingPlanWriteWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture,
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
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet<StubEntityReference> workSet = new LuceneIndexingPlanWriteWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		RuntimeException workException = new RuntimeException( "Some message" );
		resetAll();
		processorMock.beforeWorkSet( DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andThrow( workException );
		expectWorkGetInfo( 0, 1, 2 );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( workException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// All documents from the current workset, even ones from successful works
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	@Test
	public void failure_commit() {
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet<StubEntityReference> workSet = new LuceneIndexingPlanWriteWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		RuntimeException commitException = new RuntimeException( "Some message" );
		resetAll();
		processorMock.beforeWorkSet( DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 2 ) ) ).andReturn( null );
		processorMock.afterSuccessfulWorkSet();
		expectLastCall().andThrow( commitException );
		expectWorkGetInfo( 0, 1, 2 );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( commitException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// All documents from the current workset, even ones from successful works
								entityReference( 0 ), entityReference( 1 ), entityReference( 2 )
						);
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3851")
	public void failure_workAndCreateEntityReference() {
		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> workSetFuture =
				new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet<StubEntityReference> workSet = new LuceneIndexingPlanWriteWorkSet<>(
				createWorkMocks( 3 ), entityReferenceFactoryMock, workSetFuture,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		);

		FutureAssert.assertThat( workSetFuture ).isPending();

		RuntimeException workException = new RuntimeException( "Some message" );
		RuntimeException entityReferenceFactoryException = new RuntimeException( "EntityReferenceFactory message" );
		resetAll();
		processorMock.beforeWorkSet( DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
		expect( processorMock.submit( workMocks.get( 0 ) ) ).andReturn( null );
		expect( processorMock.submit( workMocks.get( 1 ) ) ).andThrow( workException );
		expectWorkGetInfo( 0 );
		expectFailingWorkGetInfo( 1, entityReferenceFactoryException );
		expectWorkGetInfo( 2 );
		replayAll();
		workSet.submitTo( processorMock );
		verifyAll();

		FutureAssert.assertThat( workSetFuture ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsSame( workException );
				softly.assertThat( workException ).hasSuppressedException( entityReferenceFactoryException );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								// All documents from the current workset, even ones from successful works
								// ... except the one that we could not convert to an entity reference
								entityReference( 0 ), entityReference( 2 )
						);
			} );
		} );
	}

	private void expectWorkGetInfo(int ... ids) {
		for ( int id : ids ) {
			LuceneSingleDocumentWriteWork workMock = workMocks.get( id );
			EasyMock.expect( workMock.getInfo() ).andStubReturn( workInfo( id ) );
			EasyMock.expect( workMock.getEntityTypeName() ).andStubReturn( TYPE_NAME );
			EasyMock.expect( workMock.getEntityIdentifier() ).andStubReturn( id );
			EasyMock.expect( entityReferenceFactoryMock.createEntityReference( TYPE_NAME, id ) )
					.andReturn( entityReference( id ) );
		}
	}

	private void expectFailingWorkGetInfo(int id, Throwable thrown) {
		LuceneSingleDocumentWriteWork workMock = workMocks.get( id );
		EasyMock.expect( workMock.getInfo() ).andStubReturn( workInfo( id ) );
		EasyMock.expect( workMock.getEntityTypeName() ).andStubReturn( TYPE_NAME );
		EasyMock.expect( workMock.getEntityIdentifier() ).andStubReturn( id );
		EasyMock.expect( entityReferenceFactoryMock.createEntityReference( TYPE_NAME, id ) )
				.andThrow( thrown );
	}

	private List<LuceneSingleDocumentWriteWork> createWorkMocks(int count) {
		List<LuceneSingleDocumentWriteWork> result = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			result.add( createWorkMock() );
		}
		return result;
	}

	private <T> LuceneSingleDocumentWriteWork createWorkMock() {
		String workName = workInfo( workMocks.size() );
		LuceneSingleDocumentWriteWork workMock = createStrictMock( workName, LuceneSingleDocumentWriteWork.class );
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