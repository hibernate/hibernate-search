/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import static org.awaitility.Awaitility.await;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import org.hibernate.search.engine.reporting.ErrorHandler;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.After;
import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class BatchingExecutorTest extends EasyMockSupport {

	private static final String NAME = "executor-name";

	private final StubWorkProcessor processorMock = createMock( StubWorkProcessor.class );
	private final ErrorHandler errorHandlerMock = createMock( ErrorHandler.class );

	// To execute code asynchronously. Just use more threads than we'll ever need, we don't care about performance.
	private final ForkJoinPool asyncExecutor = new ForkJoinPool( 12 );

	private BatchingExecutor<StubWorkSet, StubWorkProcessor> executor;

	@After
	public void cleanup() {
		asyncExecutor.shutdownNow();
		executor.stop();
	}

	@Test
	public void simple_batchCompletesImmediately() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is already completed when the endBatch() method returns,
		// allowing the executor to handle the next batch immediately.
		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		// The batch is already completed when the endBatch() method returns,
		// allowing the executor to handle the next batch immediately.
		CompletableFuture<Object> batch2Future = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		workSet2Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		replayAll();
		executor.submit( workSet2Mock );
		verifyAllAsynchronously();
	}

	@Test
	public void simple_batchCompletesLater() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet3Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch2Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet2Mock.submitTo( processorMock );
		workSet3Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		replayAll();
		executor.submit( workSet2Mock );
		executor.submit( workSet3Mock );
		// Complete the first batch: the second batch should start
		batch1Future.complete( null );
		verifyAllAsynchronously();
	}

	@Test
	public void beginBatchFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		expectLastCall().andThrow( simulatedFailure );
		errorHandlerMock.handleException(
				EasyMock.contains( "Error while processing works in executor '" + NAME + "'" ),
				EasyMock.same( simulatedFailure )
		);
		// The next worksets should not be submitted to the processor: something is very wrong
		// ... but we expect the executor to try again in the next batch:
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( workSet1Mock );
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		// The executor should still try to process submitted worksets, even after a failure
		StubWorkSet workSet3Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch2Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet3Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		replayAll();
		executor.submit( workSet3Mock );
		// Complete the first batch: the second batch should start
		batch1Future.complete( null );
		verifyAllAsynchronously();
	}

	@Test
	public void submitFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet3Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		workSet2Mock.submitTo( processorMock );
		expectLastCall().andThrow( simulatedFailure );
		workSet2Mock.markAsFailed( simulatedFailure );
		// The next worksets should still be submitted to the processor
		workSet3Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( workSet1Mock );
		executor.submit( workSet2Mock );
		executor.submit( workSet3Mock );
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		// The executor should still try to process submitted worksets, even after a failure
		StubWorkSet workSet4Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch2Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet4Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		replayAll();
		executor.submit( workSet4Mock );
		// Complete the first batch: the second batch should start
		batch1Future.complete( null );
		verifyAllAsynchronously();
	}

	@Test
	public void endBatchFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		workSet2Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andThrow( simulatedFailure );
		errorHandlerMock.handleException(
				EasyMock.contains( "Error while processing works in executor '" + NAME + "'" ),
				EasyMock.same( simulatedFailure )
		);
		replayAll();
		executor.submit( workSet1Mock );
		executor.submit( workSet2Mock );
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		// The executor should still try to process submitted worksets, even after a failure
		StubWorkSet workSet4Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch2Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet4Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		replayAll();
		executor.submit( workSet4Mock );
		verifyAllAsynchronously();
	}

	@Test
	public void completion() throws InterruptedException {
		createAndStartExecutor( 2, true );

		resetAll();
		// This should not trigger any call to the mocks
		replayAll();
		CompletableFuture<?> completionAfterStart = executor.getCompletion();
		verifyAllAsynchronously();

		// Initially, there are no worksets, so works are considered completed.
		FutureAssert.assertThat( completionAfterStart ).isSuccessful();

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( workSet1Mock );
		CompletableFuture<?> completionAfterWorkSet1Submitted = executor.getCompletion();
		verifyAllAsynchronously();

		// When a batch is being processed, work are *not* considered completed.
		FutureAssert.assertThat( completionAfterWorkSet1Submitted ).isPending();

		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch2Future = new CompletableFuture<>();
		resetAll();
		replayAll();
		executor.submit( workSet2Mock );
		CompletableFuture<?> completionAfterWorkSet2Submitted = executor.getCompletion();
		verifyAllAsynchronously();

		// Adding a work doesn't change the completion if a work is still being processed.
		FutureAssert.assertThat( completionAfterWorkSet1Submitted ).isPending();
		FutureAssert.assertThat( completionAfterWorkSet2Submitted ).isPending();

		resetAll();
		processorMock.beginBatch();
		workSet2Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		replayAll();
		batch1Future.complete( null );
		verifyAllAsynchronously();

		// When a batch is completed but the queue is not empty, works are *not* considered completed.
		FutureAssert.assertThat( completionAfterWorkSet1Submitted ).isPending();
		FutureAssert.assertThat( completionAfterWorkSet2Submitted ).isPending();

		resetAll();
		// This should not trigger any call to the mocks
		replayAll();
		batch2Future.complete( null );
		verifyAllAsynchronously();

		// When a batch is completed and the queue is empty, works are finally considered completed.
		FutureAssert.assertThat( completionAfterWorkSet1Submitted ).isSuccessful();
		FutureAssert.assertThat( completionAfterWorkSet2Submitted ).isSuccessful();

		resetAll();
		// This should not trigger any call to the mocks
		replayAll();
		CompletableFuture<?> completionAfterAllWorksCompleted = executor.getCompletion();
		verifyAllAsynchronously();

		// Whenever the queue becomes empty again, works are considered completed.
		FutureAssert.assertThat( completionAfterAllWorksCompleted ).isSuccessful();
	}


	private void verifyAllAsynchronously() {
		await().untilAsserted( () -> {
			// Synchronize on the processor, like in the batching executor,
			// so that we don't trigger ConcurrentModificationException in EasyMock.
			synchronized ( processorMock ) {
				verifyAll();
			}
		} );
	}

	/*
	 * Block the executor by submitting a batch that will only complete when the returned runnable is executed.
	 * Used to give us the time to carefully craft the next batch with a specific sequence of worksets.
	 */
	private Runnable blockExecutor()
			throws InterruptedException {
		StubWorkSet blockingWorkSetMock = createMock( StubWorkSet.class );
		CompletableFuture<Object> blockingBatchFuture = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		blockingWorkSetMock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) blockingBatchFuture );
		replayAll();
		executor.submit( blockingWorkSetMock );
		verifyAllAsynchronously();
		// Return a runnable that will unblock the executor
		return () -> blockingBatchFuture.complete( null );
	}

	private void createAndStartExecutor(int maxTasksPerBatch, boolean fair) {
		this.executor = new BatchingExecutor<>(
				NAME, processorMock, maxTasksPerBatch, fair, errorHandlerMock
		);

		resetAll();
		replayAll();
		executor.start();
		verifyAll();
	}

	private interface StubWorkSet extends BatchingExecutor.WorkSet<StubWorkProcessor> {
	}

	private interface StubWorkProcessor extends BatchingExecutor.WorkProcessor {
	}

	private class SimulatedFailure extends RuntimeException {
	}
}
