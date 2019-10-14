/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

/**
 * Test worksets produced by indexing plans.
 */
public class ElasticsearchIndexingPlanWorkSetTest extends EasyMockSupport {

	private ElasticsearchWorkProcessor processorMock = createStrictMock( ElasticsearchWorkProcessor.class );

	private List<ElasticsearchWork<Void>> workMocks = new ArrayList<>();

	@Test
	public void success() {
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet workSet = new ElasticsearchIndexingPlanWorkSet(
				createWorkMocks( 3 ), workSetFuture
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

		FutureAssert.assertThat( workSetFuture ).isSuccessful();
	}

	@Test
	public void markAsFailed() {
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet workSet = new ElasticsearchIndexingPlanWorkSet(
				createWorkMocks( 3 ), workSetFuture
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
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet workSet = new ElasticsearchIndexingPlanWorkSet(
				createWorkMocks( 3 ), workSetFuture
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
		expectWorkGetInfo( 0, 1, 2 );
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

		FutureAssert.assertThat( workSetFuture ).isFailed( workException );
	}

	@Test
	public void failure_multipleWorks() {
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> work4Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet workSet = new ElasticsearchIndexingPlanWorkSet(
				createWorkMocks( 5 ), workSetFuture
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
		expectWorkGetInfo( 0, 1, 2, 3, 4 );
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

		FutureAssert.assertThat( workSetFuture ).isFailed( work1Exception );
	}

	@Test
	public void failure_sequence() {
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet workSet = new ElasticsearchIndexingPlanWorkSet(
				createWorkMocks( 3 ), workSetFuture
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
		expectWorkGetInfo( 0, 1, 2 );
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

		FutureAssert.assertThat( workSetFuture ).isFailed( sequenceException );
	}

	@Test
	public void failure_workAndSequence() {
		CompletableFuture<Void> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> workSequenceFuture = new CompletableFuture<>();
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();

		ElasticsearchIndexingPlanWorkSet workSet = new ElasticsearchIndexingPlanWorkSet(
				createWorkMocks( 3 ), workSetFuture
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
		expectWorkGetInfo( 0, 1, 2 );
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

		FutureAssert.assertThat( workSetFuture ).isFailed( sequenceException );
	}

	private void expectWorkGetInfo(int ... ids) {
		for ( int id : ids ) {
			ElasticsearchWork<?> workMock = workMocks.get( id );
			EasyMock.expect( workMock.getInfo() ).andStubReturn( workInfo( id ) );
		}
	}

	private List<ElasticsearchWork<?>> createWorkMocks(int count) {
		List<ElasticsearchWork<?>> result = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			result.add( createWorkMock() );
		}
		return result;
	}

	private ElasticsearchWork<Void> createWorkMock() {
		String workName = workInfo( workMocks.size() );
		ElasticsearchWork<Void> workMock = createStrictMock( workName, ElasticsearchWork.class );
		workMocks.add( workMock );
		return workMock;
	}

	private String workInfo(int index) {
		return "work_" + index;
	}

}