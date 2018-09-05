/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.hibernate.search.test.util.FutureAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkAggregator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class SerialChangesetsElasticsearchWorkOrchestratorTest {

	private ElasticsearchWorkSequenceBuilder sequenceBuilderMock;
	private ElasticsearchWorkBulker bulkerMock;

	private final List<Object> mocks = new ArrayList<>();

	@Before
	public void initMocks() {
		sequenceBuilderMock = EasyMock.createStrictMock( ElasticsearchWorkSequenceBuilder.class );
		mocks.add( sequenceBuilderMock );
		bulkerMock = EasyMock.createStrictMock( ElasticsearchWorkBulker.class );
		mocks.add( bulkerMock );
	}

	@Test
	public void simple() {
		ElasticsearchWork<?> work1 = work( 1 );
		BulkableElasticsearchWork<?> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> changeset1 = Arrays.asList( work1, work2 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replay();
		SerialChangesetsElasticsearchWorkOrchestrator orchestrator =
				new SerialChangesetsElasticsearchWorkOrchestrator( sequenceBuilderMock, bulkerMock );
		verify();

		reset();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.flushBulked() ).andReturn( false );
		sequenceBuilderMock.addNonBulkExecution( work1 );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work2 ) );
		bulkerMock.add( work2 );
		expect( bulkerMock.flushBulked() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		replay();
		CompletableFuture<Void> returnedSequenceFuture = orchestrator.submit( changeset1 );
		verify();
		assertThat( returnedSequenceFuture ).isSameAs( sequenceFuture );

		reset();
		bulkerMock.flushBulk();
		replay();
		CompletableFuture<Void> futureAll = orchestrator.flush();
		verify();
		assertThat( futureAll ).isSameAs( sequenceFuture );
	}

	@Test
	public void newSequenceBetweenChangeset() {
		ElasticsearchWork<?> work1 = work( 1 );
		List<ElasticsearchWork<?>> changeset1 = Arrays.asList( work1 );

		BulkableElasticsearchWork<?> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> changeset2 = Arrays.asList( work2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replay();
		SerialChangesetsElasticsearchWorkOrchestrator orchestrator =
				new SerialChangesetsElasticsearchWorkOrchestrator( sequenceBuilderMock, bulkerMock );
		verify();

		reset();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.flushBulked() ).andReturn( false );
		sequenceBuilderMock.addNonBulkExecution( work1 );
		expect( bulkerMock.flushBulked() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replay();
		CompletableFuture<Void> returnedSequence1Future = orchestrator.submit( changeset1 );
		verify();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		reset();
		sequenceBuilderMock.init( sequence1Future );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work2 ) );
		bulkerMock.add( work2 );
		expect( bulkerMock.flushBulked() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replay();
		CompletableFuture<Void> returnedSequence2Future = orchestrator.submit( changeset2 );
		verify();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		reset();
		bulkerMock.flushBulk();
		replay();
		CompletableFuture<Void> futureAll = orchestrator.flush();
		verify();
		assertThat( futureAll ).isSameAs( sequence2Future );
	}

	@Test
	public void reuseBulkAccrossSequences() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		List<ElasticsearchWork<?>> changeset1 = Arrays.asList( work1 );

		BulkableElasticsearchWork<?> work2 = bulkableWork( 2 );
		List<ElasticsearchWork<?>> changeset2 = Arrays.asList( work2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replay();
		SerialChangesetsElasticsearchWorkOrchestrator orchestrator =
				new SerialChangesetsElasticsearchWorkOrchestrator( sequenceBuilderMock, bulkerMock );
		verify();

		reset();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work1 ) );
		bulkerMock.add( work1 );
		expect( bulkerMock.flushBulked() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replay();
		CompletableFuture<Void> returnedSequence1Future = orchestrator.submit( changeset1 );
		verify();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		reset();
		sequenceBuilderMock.init( sequence1Future );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work2 ) );
		bulkerMock.add( work2 );
		expect( bulkerMock.flushBulked() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replay();
		CompletableFuture<Void> returnedSequence2Future = orchestrator.submit( changeset2 );
		verify();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		reset();
		bulkerMock.flushBulk();
		replay();
		CompletableFuture<Void> futureAll = orchestrator.flush();
		verify();
		assertThat( futureAll ).isSameAs( sequence2Future );
	}

	@Test
	public void newBulkIfNonBulkable_sameChangeset() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		ElasticsearchWork<?> work2 = work( 2 );
		BulkableElasticsearchWork<?> work3 = bulkableWork( 3 );
		List<ElasticsearchWork<?>> changeset1 = Arrays.asList( work1, work2, work3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();

		replay();
		SerialChangesetsElasticsearchWorkOrchestrator orchestrator =
				new SerialChangesetsElasticsearchWorkOrchestrator( sequenceBuilderMock, bulkerMock );
		verify();

		reset();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work1 ) );
		bulkerMock.add( work1 );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.flushBulked() ).andReturn( true );
		sequenceBuilderMock.addNonBulkExecution( work2 );
		work3.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work3 ) );
		bulkerMock.flushBulk();
		bulkerMock.add( work3 );
		expect( bulkerMock.flushBulked() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replay();
		CompletableFuture<Void> returnedSequence1Future = orchestrator.submit( changeset1 );
		verify();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		reset();
		bulkerMock.flushBulk();
		replay();
		CompletableFuture<Void> futureAll = orchestrator.flush();
		verify();
		assertThat( futureAll ).isSameAs( sequence1Future );
	}

	@Test
	public void newBulkIfNonBulkable_differenceChangesets() {
		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		List<ElasticsearchWork<?>> changeset1 = Arrays.asList( work1 );
		ElasticsearchWork<?> work2 = work( 2 );
		BulkableElasticsearchWork<?> work3 = bulkableWork( 3 );
		List<ElasticsearchWork<?>> changeset2 = Arrays.asList( work2, work3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replay();
		SerialChangesetsElasticsearchWorkOrchestrator orchestrator =
				new SerialChangesetsElasticsearchWorkOrchestrator( sequenceBuilderMock, bulkerMock );
		verify();

		reset();
		sequenceBuilderMock.init( anyObject() );
		work1.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work1 ) );
		bulkerMock.add( work1 );
		expect( bulkerMock.flushBulked() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replay();
		CompletableFuture<Void> returnedSequence1Future = orchestrator.submit( changeset1 );
		verify();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		reset();
		sequenceBuilderMock.init( anyObject() );
		work2.aggregate( anyObject() );
		expectLastCall().andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.flushBulked() ).andReturn( true );
		sequenceBuilderMock.addNonBulkExecution( work2 );
		work3.aggregate( anyObject() );
		expectLastCall().andAnswer( bulkableAggregateAnswer( work3 ) );
		bulkerMock.flushBulk();
		bulkerMock.add( work3 );
		expect( bulkerMock.flushBulked() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replay();
		CompletableFuture<Void> returnedSequence2Future = orchestrator.submit( changeset2 );
		verify();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		reset();
		bulkerMock.flushBulk();
		replay();
		CompletableFuture<Void> futureAll = orchestrator.flush();
		verify();
		assertThat( futureAll ).isSameAs( sequence2Future );
	}

	private void reset() {
		EasyMock.reset( mocks.toArray() );
	}

	private void replay() {
		EasyMock.replay( mocks.toArray() );
	}

	private void verify() {
		EasyMock.verify( mocks.toArray() );
	}

	private ElasticsearchWork<?> work(int index) {
		ElasticsearchWork<?> mock = EasyMock.createStrictMock( "work" + index, ElasticsearchWork.class );
		mocks.add( mock );
		return mock;
	}

	private BulkableElasticsearchWork<?> bulkableWork(int index) {
		BulkableElasticsearchWork<?> mock = EasyMock.createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
		mocks.add( mock );
		return mock;
	}

	private IAnswer<Void> nonBulkableAggregateAnswer(ElasticsearchWork<?> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			aggregator.addNonBulkable( mock );
			return null;
		};
	}

	private IAnswer<Void> bulkableAggregateAnswer(BulkableElasticsearchWork<?> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			aggregator.addBulkable( mock );
			return null;
		};
	}
}
