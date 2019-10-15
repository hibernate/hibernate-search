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
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkProcessor;
import org.hibernate.search.backend.lucene.search.impl.LuceneDocumentReference;
import org.hibernate.search.backend.lucene.work.impl.LuceneSingleDocumentWriteWork;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Test;

import org.assertj.core.api.SoftAssertions;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

/**
 * Test worksets produced by indexing plans.
 */
public class LuceneIndexingPlanWriteWorkSetTest extends EasyMockSupport {

	private static final String INDEX_NAME = "SomeIndexName";

	private LuceneWriteWorkProcessor processorMock = createStrictMock( LuceneWriteWorkProcessor.class );

	private List<LuceneSingleDocumentWriteWork<?>> workMocks = new ArrayList<>();

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
		CompletableFuture<IndexIndexingPlanExecutionReport> workSetFuture = new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet workSet = new LuceneIndexingPlanWriteWorkSet(
				INDEX_NAME, createWorkMocks( 3 ), workSetFuture,
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
				softly.assertThat( report.getFailingDocuments() ).isEmpty();
			} );
		} );
	}

	@Test
	public void markAsFailed() {
		CompletableFuture<IndexIndexingPlanExecutionReport> workSetFuture = new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet workSet = new LuceneIndexingPlanWriteWorkSet(
				INDEX_NAME, createWorkMocks( 3 ), workSetFuture,
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
		CompletableFuture<IndexIndexingPlanExecutionReport> workSetFuture = new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet workSet = new LuceneIndexingPlanWriteWorkSet(
				INDEX_NAME, createWorkMocks( 3 ), workSetFuture,
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
				softly.assertThat( report.getFailingDocuments() )
						.containsExactly(
								// All documents from the current workset, even ones from successful works
								docReference( 0 ), docReference( 1 ), docReference( 2 )
						);
			} );
		} );
	}

	@Test
	public void failure_commit() {
		CompletableFuture<IndexIndexingPlanExecutionReport> workSetFuture = new CompletableFuture<>();

		LuceneIndexingPlanWriteWorkSet workSet = new LuceneIndexingPlanWriteWorkSet(
				INDEX_NAME, createWorkMocks( 3 ), workSetFuture,
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
				softly.assertThat( report.getFailingDocuments() )
						.containsExactly(
								// All documents from the current workset, even ones from successful works
								docReference( 0 ), docReference( 1 ), docReference( 2 )
						);
			} );
		} );
	}

	private void expectWorkGetInfo(int ... ids) {
		for ( int id : ids ) {
			LuceneSingleDocumentWriteWork<?> workMock = workMocks.get( id );
			EasyMock.expect( workMock.getInfo() ).andStubReturn( workInfo( id ) );
			EasyMock.expect( workMock.getDocumentId() ).andStubReturn( String.valueOf( id ) );
		}
	}

	private List<LuceneSingleDocumentWriteWork<?>> createWorkMocks(int count) {
		List<LuceneSingleDocumentWriteWork<?>> result = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			result.add( createWorkMock() );
		}
		return result;
	}

	private <T> LuceneSingleDocumentWriteWork<T> createWorkMock() {
		String workName = workInfo( workMocks.size() );
		LuceneSingleDocumentWriteWork<T> workMock = createStrictMock( workName, LuceneSingleDocumentWriteWork.class );
		workMocks.add( workMock );
		return workMock;
	}

	private DocumentReference docReference(int id) {
		return new LuceneDocumentReference( INDEX_NAME, String.valueOf( id ) );
	}

	private String workInfo(int index) {
		return "work_" + index;
	}

}